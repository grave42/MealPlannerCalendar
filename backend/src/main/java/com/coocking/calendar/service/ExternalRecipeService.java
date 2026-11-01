package com.coocking.calendar.service;

import com.coocking.calendar.config.properties.ExternalRecipeProperties;
import com.coocking.calendar.service.external.ExternalRecipe;
import com.coocking.calendar.service.external.ExternalRecipeSearchItem;
import com.coocking.calendar.service.external.RecipeSourceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Оркестрация поиска и загрузки рецептов; не знает о конкретном источнике.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalRecipeService {

    private final RecipeSourceClient recipeSourceClient;
    private final ExternalRecipeProperties properties;

    /**
     * Ищет рецепты по названию (не более {@code app.external-recipe.max-results}).
     */
    public Flux<ExternalRecipeSearchItem> search(String query) {
        int limit = properties.getMaxResults();
        return Mono.deferContextual(Mono::just)
                .flatMapMany(ctx -> recipeSourceClient.search(query, limit))
                .take(limit)
                .doOnComplete(() -> log.debug("Поиск рецептов в источнике {} завершён, запрос='{}'",
                        recipeSourceClient.getSourceName(), query))
                .doOnError(e -> log.error("Ошибка поиска рецептов в источнике {}, запрос='{}'",
                        recipeSourceClient.getSourceName(), query));
    }

    /**
     * Загружает рецепт по идентификатору из результатов поиска.
     */
    public Mono<ExternalRecipe> getById(String id) {
        return Mono.deferContextual(Mono::just)
                .flatMap(ctx -> recipeSourceClient.getById(id))
                .doOnSuccess(recipe -> log.debug("Рецепт {} загружен из источника {}",
                        id, recipeSourceClient.getSourceName()))
                .doOnError(e -> log.error("Ошибка загрузки рецепта {} из источника {}",
                        id, recipeSourceClient.getSourceName()));
    }
}
