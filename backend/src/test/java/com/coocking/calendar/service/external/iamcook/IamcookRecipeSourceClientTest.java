package com.coocking.calendar.service.external.iamcook;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.coocking.calendar.config.properties.ExternalRecipeProperties;
import com.coocking.calendar.exception.BadRequestException;
import com.coocking.calendar.exception.ExternalServiceException;
import com.coocking.calendar.exception.NotFoundException;
import com.coocking.calendar.service.external.ExternalRecipe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Класс IamcookRecipeSourceClient")
class IamcookRecipeSourceClientTest {

    private static final ExchangeStrategies STRATEGIES = ExchangeStrategies.builder()
            .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
            .build();

    /** Подменённый транспорт: запоминает запросы и отвечает заданным образом. */
    private static final class StubExchange implements ExchangeFunction {

        private final Function<ClientRequest, Mono<ClientResponse>> responder;
        private final List<ClientRequest> requests = Collections.synchronizedList(new ArrayList<>());
        private final List<Long> times = Collections.synchronizedList(new ArrayList<>());

        private StubExchange(Function<ClientRequest, Mono<ClientResponse>> responder) {
            this.responder = responder;
        }

        @Override
        public Mono<ClientResponse> exchange(ClientRequest request) {
            requests.add(request);
            times.add(System.nanoTime());
            return responder.apply(request);
        }
    }

    private byte[] fixture(String name) {
        try {
            return new ClassPathResource("external-recipe/" + name).getInputStream().readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private Mono<ClientResponse> html(String body) {
        return Mono.just(ClientResponse.create(HttpStatus.OK, STRATEGIES)
                .header(HttpHeaders.CONTENT_TYPE, "text/html;charset=UTF-8")
                .body(body)
                .build());
    }

    private Mono<ClientResponse> fixtureResponse(String name) {
        return html(new String(fixture(name), StandardCharsets.UTF_8));
    }

    private Mono<ClientResponse> status(HttpStatus status) {
        return Mono.just(ClientResponse.create(status, STRATEGIES).body("error").build());
    }

    private ExternalRecipeProperties createProperties() {
        ExternalRecipeProperties properties = new ExternalRecipeProperties();
        properties.setMinRequestInterval(Duration.ZERO);
        properties.setTimeout(Duration.ofSeconds(2));
        return properties;
    }

    private IamcookRecipeSourceClient createClient(StubExchange exchange, ExternalRecipeProperties properties) {
        WebClient webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .exchangeFunction(exchange)
                .build();
        return new IamcookRecipeSourceClient(webClient, properties);
    }

    private IamcookRecipeSourceClient createClient(StubExchange exchange) {
        return createClient(exchange, createProperties());
    }

    @Nested
    @DisplayName("Поиск")
    class Search {

        @Test
        @DisplayName("Несколько результатов ограничиваются лимитом")
        void testMultiple() {
            StubExchange exchange = new StubExchange(r -> fixtureResponse("search-multiple.html"));

            StepVerifier.create(createClient(exchange).search("паста", 10).collectList())
                    .assertNext(items -> {
                        assertEquals(10, items.size());
                        items.forEach(item -> {
                            assertTrue(item.getId().matches("\\d+"));
                            assertFalse(item.getTitle().isBlank());
                            assertTrue(item.getImageUrl().startsWith("https://img.iamcook.ru/"));
                        });
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Один результат")
        void testSingle() {
            StubExchange exchange = new StubExchange(r -> fixtureResponse("search-single.html"));

            StepVerifier.create(createClient(exchange).search("паста аль помодоро", 10))
                    .assertNext(item -> {
                        assertEquals("37207", item.getId());
                        assertEquals("Паста аль-помодоро", item.getTitle());
                        assertEquals("https://img.iamcook.ru/2024/upl/recipes/middle/"
                                + "u-3c62feb49986efb4ac0208f359a78d00.jpg", item.getImageUrl());
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Нет результатов — пустой поток")
        void testEmpty() {
            StubExchange exchange = new StubExchange(r -> fixtureResponse("search-empty.html"));

            StepVerifier.create(createClient(exchange).search("жжжж", 10)).verifyComplete();
        }

        @Test
        @DisplayName("Запрос — POST /advsearch с полем sbfield в UTF-8")
        void testRequestShape() {
            StubExchange exchange = new StubExchange(r -> fixtureResponse("search-empty.html"));

            StepVerifier.create(createClient(exchange).search("паста", 10)).verifyComplete();

            ClientRequest request = exchange.requests.get(0);
            assertEquals(HttpMethod.POST, request.method());
            assertEquals("https://www.iamcook.ru/advsearch", request.url().toString());
            MockClientHttpRequest mock = new MockClientHttpRequest(HttpMethod.POST, "/");
            BodyInserter.Context context = new BodyInserter.Context() {
                @Override
                public List<HttpMessageWriter<?>> messageWriters() {
                    return STRATEGIES.messageWriters();
                }

                @Override
                public Optional<org.springframework.http.server.reactive.ServerHttpRequest> serverRequest() {
                    return Optional.empty();
                }

                @Override
                public Map<String, Object> hints() {
                    return Map.of();
                }
            };
            StepVerifier.create(request.body().insert(mock, context).then(Mono.defer(mock::getBodyAsString)))
                    .expectNext("sbfield=%D0%BF%D0%B0%D1%81%D1%82%D0%B0")
                    .verifyComplete();
            assertTrue(String.valueOf(mock.getHeaders().getContentType()).toLowerCase()
                    .contains("application/x-www-form-urlencoded"));
        }

        @Test
        @DisplayName("Ответ 200 с PHP Fatal error — ExternalServiceException")
        void testPhpFatalError() {
            StubExchange exchange = new StubExchange(r -> html(
                    "<html><body><div id=\"advsearch\"><br /><b>Fatal error</b>: Uncaught ValueError</div></body></html>"));

            StepVerifier.create(createClient(exchange).search("паста", 10))
                    .expectError(ExternalServiceException.class)
                    .verify();
        }

        @Test
        @DisplayName("Изменившаяся вёрстка (нет контейнера результатов) — ExternalServiceException")
        void testLayoutChanged() {
            StubExchange exchange = new StubExchange(r -> html("<html><body><p>Другая страница</p></body></html>"));

            StepVerifier.create(createClient(exchange).search("паста", 10))
                    .expectError(ExternalServiceException.class)
                    .verify();
        }

        @Test
        @DisplayName("Счётчик найденных больше нуля, но карточек нет — ExternalServiceException")
        void testCountWithoutCards() {
            StubExchange exchange = new StubExchange(r -> html("<div id=\"advsearch\">"
                    + "<p class=\"scnotice\">По запросу найдено рецептов: 5</p></div>"));

            StepVerifier.create(createClient(exchange).search("паста", 10))
                    .expectError(ExternalServiceException.class)
                    .verify();
        }

        @Test
        @DisplayName("Ответ 5xx — ExternalServiceException с userMessage")
        void testServerError() {
            StubExchange exchange = new StubExchange(r -> status(HttpStatus.SERVICE_UNAVAILABLE));

            StepVerifier.create(createClient(exchange).search("паста", 10))
                    .expectErrorSatisfies(e -> {
                        assertTrue(e instanceof ExternalServiceException);
                        assertEquals("Не удалось получить данные с iamcook.ru. "
                                + "Попробуйте позже или заполните блюдо вручную",
                                ((ExternalServiceException) e).getUserMessage());
                    })
                    .verify();
        }

        @Test
        @DisplayName("Таймаут — ExternalServiceException")
        void testTimeout() {
            ExternalRecipeProperties properties = createProperties();
            properties.setTimeout(Duration.ofMillis(100));
            StubExchange exchange = new StubExchange(r -> Mono.never());

            StepVerifier.create(createClient(exchange, properties).search("паста", 10))
                    .expectError(ExternalServiceException.class)
                    .verify(Duration.ofSeconds(3));
        }
    }

    @Nested
    @DisplayName("Загрузка рецепта")
    class GetById {

        @Test
        @DisplayName("Разбор страницы рецепта: все поля, фото шагов не попадают в текст")
        void testFullRecipe() {
            StubExchange exchange = new StubExchange(r -> fixtureResponse("recipe-37207.html"));

            StepVerifier.create(createClient(exchange).getById("37207"))
                    .assertNext(recipe -> {
                        assertEquals("37207", recipe.getId());
                        assertEquals("Паста аль-помодоро", recipe.getTitle());
                        assertTrue(recipe.getDescription().startsWith("Итальянскую кухню"));
                        assertTrue(recipe.getIngredients().contains("\nСоус:\n"));
                        assertTrue(recipe.getIngredients().contains("Паста - 450 г"));
                        assertTrue(recipe.getIngredients().lines().count() >= 8);
                        assertFalse(recipe.getInstructions().isBlank());
                        assertTrue(recipe.getInstructions().lines().count() >= 3);
                        assertFalse(recipe.getInstructions().contains("img.iamcook.ru"));
                        assertFalse(recipe.getInstructions().contains("<"));
                        assertEquals("https://img.iamcook.ru/2024/upl/recipes/zen/"
                                + "u-3c62feb49986efb4ac0208f359a78d00.jpg", recipe.getImageUrl());
                    })
                    .verifyComplete();
            assertEquals("https://www.iamcook.ru/showrecipe/37207", exchange.requests.get(0).url().toString());
            assertEquals(HttpMethod.GET, exchange.requests.get(0).method());
        }

        @Test
        @DisplayName("Часть полей отсутствует — они равны null")
        void testPartialRecipe() {
            StubExchange exchange = new StubExchange(r -> html(
                    "<html><body><div id=\"recbody\"><h1>Только название</h1></div></body></html>"));

            StepVerifier.create(createClient(exchange).getById("1"))
                    .assertNext(recipe -> {
                        assertEquals("Только название", recipe.getTitle());
                        assertNull(recipe.getDescription());
                        assertNull(recipe.getIngredients());
                        assertNull(recipe.getInstructions());
                        assertNull(recipe.getImageUrl());
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Картинка берётся из JSON-LD, если нет og:image")
        void testImageFromJsonLd() {
            StubExchange exchange = new StubExchange(r -> html("<html><head>"
                    + "<script type=\"application/ld+json\">{\"@type\":\"Recipe\","
                    + "\"image\":[\"https://img.iamcook.ru/x.jpg\"]}</script></head>"
                    + "<body><div id=\"recbody\"><h1>Рецепт</h1></div></body></html>"));

            StepVerifier.create(createClient(exchange).getById("1"))
                    .assertNext(recipe -> assertEquals("https://img.iamcook.ru/x.jpg", recipe.getImageUrl()))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Нет заголовка рецепта — ExternalServiceException")
        void testNoTitle() {
            StubExchange exchange = new StubExchange(r -> html("<html><body>Пусто</body></html>"));

            StepVerifier.create(createClient(exchange).getById("1"))
                    .expectError(ExternalServiceException.class)
                    .verify();
        }

        @Test
        @DisplayName("Нецифровой id — BadRequestException без исходящего запроса")
        void testInvalidId() {
            StubExchange exchange = new StubExchange(r -> fixtureResponse("recipe-37207.html"));
            IamcookRecipeSourceClient client = createClient(exchange);

            for (String id : new String[]{"../../etc/passwd", "12a", "", " ", "1/2", "37207?x=1", "%2e%2e"}) {
                StepVerifier.create(client.getById(id)).expectError(BadRequestException.class).verify();
            }
            StepVerifier.create(client.getById(null)).expectError(BadRequestException.class).verify();

            assertTrue(exchange.requests.isEmpty());
        }

        @Test
        @DisplayName("Ответ 404 — NotFoundException")
        void testNotFound() {
            StubExchange exchange = new StubExchange(r -> status(HttpStatus.NOT_FOUND));

            StepVerifier.create(createClient(exchange).getById("999999999"))
                    .expectError(NotFoundException.class)
                    .verify();
        }

        @Test
        @DisplayName("Ответ 5xx — ExternalServiceException")
        void testServerError() {
            StubExchange exchange = new StubExchange(r -> status(HttpStatus.BAD_GATEWAY));

            StepVerifier.create(createClient(exchange).getById("1"))
                    .expectError(ExternalServiceException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("Скачивание картинки")
    class DownloadImage {

        private Mono<ClientResponse> imageResponse(byte[] bytes) {
            return Mono.just(ClientResponse.create(HttpStatus.OK, STRATEGIES)
                    .header(HttpHeaders.CONTENT_TYPE, "image/jpeg")
                    .body(Flux.just(org.springframework.core.io.buffer.DefaultDataBufferFactory.sharedInstance
                            .wrap(bytes)))
                    .build());
        }

        @Test
        @DisplayName("Картинка с хоста *.iamcook.ru скачивается")
        void testOk() {
            StubExchange exchange = new StubExchange(r -> imageResponse(new byte[]{1, 2, 3}));

            StepVerifier.create(createClient(exchange).downloadImage("https://img.iamcook.ru/a.jpg"))
                    .assertNext(image -> {
                        assertEquals(3, image.getContent().length);
                        assertEquals("image/jpeg", image.getContentType());
                    })
                    .verifyComplete();
            assertEquals(URI.create("https://img.iamcook.ru/a.jpg"), exchange.requests.get(0).url());
        }

        @Test
        @DisplayName("Чужой хост, схема или userinfo — BadRequestException без запроса")
        void testForeignHost() {
            StubExchange exchange = new StubExchange(r -> imageResponse(new byte[]{1}));
            IamcookRecipeSourceClient client = createClient(exchange);

            for (String url : new String[]{"https://evil.example/a.jpg", "https://iamcook.ru.evil.com/a.jpg",
                    "https://eviliamcook.ru/a.jpg", "http://img.iamcook.ru/a.jpg", "https://user@img.iamcook.ru/a.jpg",
                    "https://img.iamcook.ru:8443/a.jpg", "file:///etc/passwd", "not a url", ""}) {
                StepVerifier.create(client.downloadImage(url)).expectError(BadRequestException.class).verify();
            }
            StepVerifier.create(client.downloadImage(null)).expectError(BadRequestException.class).verify();

            assertTrue(exchange.requests.isEmpty());
        }

        @Test
        @DisplayName("Картинка больше лимита — ExternalServiceException")
        void testTooLarge() {
            ExternalRecipeProperties properties = createProperties();
            properties.setMaxImageSize(DataSize.ofBytes(4));
            StubExchange exchange = new StubExchange(r -> imageResponse(new byte[100]));

            StepVerifier.create(createClient(exchange, properties).downloadImage("https://img.iamcook.ru/a.jpg"))
                    .expectError(ExternalServiceException.class)
                    .verify();
        }

        @Test
        @DisplayName("Ответ 404 — ExternalServiceException")
        void testHttpError() {
            StubExchange exchange = new StubExchange(r -> status(HttpStatus.NOT_FOUND));

            StepVerifier.create(createClient(exchange).downloadImage("https://img.iamcook.ru/a.jpg"))
                    .expectError(ExternalServiceException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("Ограничение частоты запросов")
    class RateLimit {

        @Test
        @DisplayName("Параллельные запросы выстраиваются с минимальным интервалом")
        void testMinInterval() {
            ExternalRecipeProperties properties = createProperties();
            properties.setMinRequestInterval(Duration.ofMillis(300));
            AtomicInteger counter = new AtomicInteger();
            StubExchange exchange = new StubExchange(r -> {
                counter.incrementAndGet();
                return html("<div id=\"recbody\"><h1>Рецепт</h1></div>");
            });
            IamcookRecipeSourceClient client = createClient(exchange, properties);

            StepVerifier.create(Flux.merge(client.getById("1"), client.getById("2"), client.getById("3"))
                            .map(ExternalRecipe::getId).collectList())
                    .assertNext(ids -> assertEquals(3, ids.size()))
                    .verifyComplete();

            assertEquals(3, counter.get());
            assertNotNull(exchange.times);
            long first = exchange.times.get(0);
            long second = exchange.times.get(1);
            long third = exchange.times.get(2);
            assertTrue(second - first >= Duration.ofMillis(250).toNanos(), "второй запрос слишком рано");
            assertTrue(third - second >= Duration.ofMillis(250).toNanos(), "третий запрос слишком рано");
        }
    }
}
