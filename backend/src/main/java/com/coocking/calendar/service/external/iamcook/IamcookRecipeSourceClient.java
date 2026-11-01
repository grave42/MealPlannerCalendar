package com.coocking.calendar.service.external.iamcook;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.coocking.calendar.config.properties.ExternalRecipeProperties;
import com.coocking.calendar.exception.BadRequestException;
import com.coocking.calendar.exception.CalendarException;
import com.coocking.calendar.exception.ExternalServiceException;
import com.coocking.calendar.exception.NotFoundException;
import com.coocking.calendar.service.external.ExternalImage;
import com.coocking.calendar.service.external.ExternalRecipe;
import com.coocking.calendar.service.external.ExternalRecipeSearchItem;
import com.coocking.calendar.service.external.RecipeSourceClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Источник рецептов iamcook.ru: поиск через {@code POST /advsearch}, рецепт через {@code GET /showrecipe/<id>}.
 * Парсинг HTML выполняется jsoup вне event loop; исходящие запросы ограничены по частоте.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.external-recipe", name = "source", havingValue = "iamcook")
public class IamcookRecipeSourceClient implements RecipeSourceClient {

    private static final String SOURCE_NAME = "iamcook.ru";
    private static final String USER_MESSAGE = "Не удалось получить данные с iamcook.ru. "
            + "Попробуйте позже или заполните блюдо вручную";
    private static final Pattern ID_PATTERN = Pattern.compile("\\d{1,12}");
    private static final Pattern RECIPE_HREF_PATTERN = Pattern.compile("/showrecipe/(\\d{1,12})");
    private static final Pattern POSITIVE_COUNT_PATTERN = Pattern.compile(":\\s*([1-9]\\d*)\\s*$");
    private static final String SEARCH_FIELD = "sbfield";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WebClient webClient;
    private final ExternalRecipeProperties properties;
    private final RequestRateLimiter rateLimiter;

    public IamcookRecipeSourceClient(@Qualifier("externalRecipeWebClient") WebClient webClient,
                                     ExternalRecipeProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
        this.rateLimiter = new RequestRateLimiter(properties.getMinRequestInterval());
    }

    @Override
    public Flux<ExternalRecipeSearchItem> search(String query, int limit) {
        return Mono.deferContextual(Mono::just)
                .flatMap(ctx -> rateLimiter.acquire().then(fetchSearchPage(query)))
                .flatMap(html -> parseOffloaded(() -> parseSearchPage(html, limit)))
                .doOnSuccess(items -> log.debug("Поиск в {}: запрос='{}', найдено {}", SOURCE_NAME, query,
                        Objects.isNull(items) ? 0 : items.size()))
                .doOnError(e -> log.error("Ошибка поиска в {}, запрос='{}'", SOURCE_NAME, query, e))
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Mono<ExternalRecipe> getById(String id) {
        if (Objects.isNull(id) || !ID_PATTERN.matcher(id).matches()) {
            return Mono.error(new BadRequestException("Invalid recipe id for " + SOURCE_NAME,
                    "Некорректный идентификатор рецепта"));
        }
        return Mono.deferContextual(Mono::just)
                .flatMap(ctx -> rateLimiter.acquire().then(fetchRecipePage(id)))
                .flatMap(html -> parseOffloaded(() -> parseRecipePage(id, html)))
                .doOnSuccess(recipe -> log.debug("Рецепт {} загружен из {}", id, SOURCE_NAME))
                .doOnError(e -> log.error("Ошибка загрузки рецепта {} из {}", id, SOURCE_NAME, e));
    }

    @Override
    public Mono<ExternalImage> downloadImage(String imageUrl) {
        URI uri;
        try {
            uri = validateImageUri(imageUrl);
        } catch (BadRequestException e) {
            return Mono.error(e);
        }
        long maxSize = properties.getMaxImageSize().toBytes();
        return Mono.deferContextual(Mono::just)
                .flatMap(ctx -> rateLimiter.acquire().then(webClient.get().uri(uri)
                        .exchangeToMono(response -> {
                            if (!response.statusCode().is2xxSuccessful()) {
                                return Mono.<ExternalImage>error(new ExternalServiceException(
                                        "Image request to " + SOURCE_NAME + " failed with status "
                                                + response.statusCode().value(), USER_MESSAGE));
                            }
                            String contentType = response.headers().contentType()
                                    .map(MediaType::toString).orElse(null);
                            return DataBufferUtils.join(response.bodyToFlux(DataBuffer.class), (int) maxSize)
                                    .map(buffer -> toExternalImage(buffer, contentType));
                        })
                        .timeout(properties.getTimeout())))
                .onErrorMap(e -> !(e instanceof CalendarException), this::toExternalServiceException)
                .doOnSuccess(image -> log.debug("Картинка скачана из {}: {}", SOURCE_NAME, imageUrl))
                .doOnError(e -> log.warn("Не удалось скачать картинку из {}: {}", SOURCE_NAME, imageUrl));
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }

    private Mono<String> fetchSearchPage(String query) {
        return webClient.post()
                .uri(properties.getSearchPath())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(SEARCH_FIELD, query))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(properties.getTimeout())
                .onErrorMap(e -> !(e instanceof CalendarException), this::toExternalServiceException);
    }

    private Mono<String> fetchRecipePage(String id) {
        return webClient.get()
                .uri(properties.getRecipePath() + id)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(properties.getTimeout())
                .onErrorMap(WebClientResponseException.NotFound.class,
                        e -> new NotFoundException("Recipe " + id + " not found in " + SOURCE_NAME,
                                "Рецепт не найден на iamcook.ru"))
                .onErrorMap(e -> !(e instanceof CalendarException), this::toExternalServiceException);
    }

    private ExternalServiceException toExternalServiceException(Throwable e) {
        String reason = e instanceof TimeoutException ? "timeout" : e.getClass().getSimpleName();
        if (e instanceof DataBufferLimitException) {
            reason = "response is too large";
        }
        return new ExternalServiceException(SOURCE_NAME + " request failed: " + reason + ": " + e.getMessage(),
                USER_MESSAGE, e);
    }

    private <T> Mono<T> parseOffloaded(Callable<T> parser) {
        return Mono.fromCallable(parser).subscribeOn(Schedulers.boundedElastic());
    }

    private ExternalImage toExternalImage(DataBuffer buffer, String contentType) {
        try {
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            return ExternalImage.builder().content(bytes).contentType(contentType).build();
        } finally {
            DataBufferUtils.release(buffer);
        }
    }

    private URI validateImageUri(String imageUrl) {
        URI uri;
        try {
            uri = new URI(Objects.requireNonNullElse(imageUrl, "").trim());
        } catch (URISyntaxException e) {
            throw invalidImageUrl("malformed url");
        }
        String host = uri.getHost();
        String suffix = properties.getImageHostSuffix().toLowerCase(Locale.ROOT);
        boolean hostAllowed = Objects.nonNull(host)
                && (host.toLowerCase(Locale.ROOT).equals(suffix) || host.toLowerCase(Locale.ROOT).endsWith("." + suffix));
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !hostAllowed || Objects.nonNull(uri.getUserInfo())
                || (uri.getPort() != -1 && uri.getPort() != 443)) {
            throw invalidImageUrl("url does not belong to " + suffix);
        }
        return uri;
    }

    private BadRequestException invalidImageUrl(String reason) {
        return new BadRequestException("Image url rejected: " + reason,
                "Адрес картинки не принадлежит " + SOURCE_NAME);
    }

    private List<ExternalRecipeSearchItem> parseSearchPage(String html, int limit) {
        ExternalRecipeProperties.Selectors sel = properties.getSelectors();
        Document doc = Jsoup.parse(html, properties.getBaseUrl());
        Element container = doc.selectFirst(sel.getSearchContainer());
        if (Objects.isNull(container)) {
            throw layoutChanged("search container not found");
        }
        if (container.text().contains("Fatal error")) {
            throw layoutChanged("search page contains a server error");
        }
        List<Element> cards = container.select(sel.getSearchCard());
        List<ExternalRecipeSearchItem> result = new ArrayList<>();
        for (Element card : cards) {
            if (result.size() >= limit) {
                break;
            }
            ExternalRecipeSearchItem item = parseSearchCard(card, sel);
            if (Objects.nonNull(item)) {
                result.add(item);
            }
        }
        if (result.isEmpty() && (!cards.isEmpty() || hasPositiveCount(container, sel))) {
            throw layoutChanged("results are present but no card could be parsed");
        }
        return result;
    }

    private boolean hasPositiveCount(Element container, ExternalRecipeProperties.Selectors sel) {
        Element notice = container.selectFirst(sel.getSearchCount());
        return Objects.nonNull(notice) && POSITIVE_COUNT_PATTERN.matcher(notice.text().trim()).find();
    }

    private ExternalRecipeSearchItem parseSearchCard(Element card, ExternalRecipeProperties.Selectors sel) {
        Element link = card.selectFirst(sel.getSearchCardLink());
        Element title = card.selectFirst(sel.getSearchCardTitle());
        if (Objects.isNull(link) || Objects.isNull(title) || !StringUtils.hasText(title.text())) {
            return null;
        }
        Matcher matcher = RECIPE_HREF_PATTERN.matcher(link.attr("href"));
        if (!matcher.find()) {
            return null;
        }
        Element image = card.selectFirst(sel.getSearchCardImage());
        return ExternalRecipeSearchItem.builder()
                .id(matcher.group(1))
                .title(title.text().trim())
                .imageUrl(Objects.isNull(image) ? null : normalizeUrl(image.attr("src")))
                .build();
    }

    private ExternalRecipe parseRecipePage(String id, String html) {
        ExternalRecipeProperties.Selectors sel = properties.getSelectors();
        Document doc = Jsoup.parse(html, properties.getBaseUrl());
        Element title = doc.selectFirst(sel.getRecipeTitle());
        if (Objects.isNull(title) || !StringUtils.hasText(title.text())) {
            throw layoutChanged("recipe title not found");
        }
        return ExternalRecipe.builder()
                .id(id)
                .title(title.text().trim())
                .description(textOrNull(doc.selectFirst(sel.getRecipeDescription())))
                .ingredients(parseIngredients(doc.selectFirst(sel.getRecipeIngredients())))
                .instructions(parseInstructions(doc.selectFirst(sel.getRecipeInstructions())))
                .imageUrl(parseRecipeImage(doc, sel))
                .build();
    }

    private String parseIngredients(Element list) {
        if (Objects.isNull(list)) {
            return null;
        }
        List<String> lines = new ArrayList<>();
        for (Element element : list.select("p, strong")) {
            boolean heading = element.tagName().equals("strong");
            if (heading && element.parent() != null && element.parent().tagName().equals("p")) {
                continue;
            }
            if (StringUtils.hasText(element.text())) {
                lines.add(element.text().trim());
            }
        }
        return lines.isEmpty() ? null : String.join("\n", lines);
    }

    private String parseInstructions(Element block) {
        if (Objects.isNull(block)) {
            return null;
        }
        List<String> steps = new ArrayList<>();
        for (Element paragraph : block.select("p")) {
            if (!paragraph.select("div.primg, img").isEmpty() || Objects.nonNull(paragraph.closest("div.primg"))) {
                continue;
            }
            if (StringUtils.hasText(paragraph.text())) {
                steps.add(paragraph.text().trim());
            }
        }
        return steps.isEmpty() ? null : String.join("\n", steps);
    }

    private String parseRecipeImage(Document doc, ExternalRecipeProperties.Selectors sel) {
        Element meta = doc.selectFirst(sel.getRecipeImageMeta());
        if (Objects.nonNull(meta) && StringUtils.hasText(meta.attr("content"))) {
            return normalizeUrl(meta.attr("content"));
        }
        for (Element script : doc.select(sel.getRecipeJsonLd())) {
            try {
                JsonNode root = OBJECT_MAPPER.readTree(script.data());
                if (Objects.nonNull(root) && "Recipe".equals(root.path("@type").asText())) {
                    JsonNode image = root.path("image");
                    String url = image.isArray() ? image.path(0).asText(null) : image.asText(null);
                    if (StringUtils.hasText(url)) {
                        return normalizeUrl(url);
                    }
                }
            } catch (java.io.IOException e) {
                log.debug("Не удалось разобрать JSON-LD страницы рецепта: {}", e.getMessage());
            }
        }
        return null;
    }

    private String textOrNull(Element element) {
        return Objects.isNull(element) || !StringUtils.hasText(element.text()) ? null : element.text().trim();
    }

    private String normalizeUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        String trimmed = url.trim();
        if (trimmed.startsWith("//")) {
            return "https:" + trimmed;
        }
        if (trimmed.startsWith("/")) {
            return properties.getBaseUrl() + trimmed;
        }
        return trimmed;
    }

    private ExternalServiceException layoutChanged(String reason) {
        return new ExternalServiceException(SOURCE_NAME + " page layout is unexpected: " + reason, USER_MESSAGE);
    }
}
