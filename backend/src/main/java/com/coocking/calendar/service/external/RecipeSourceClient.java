package com.coocking.calendar.service.external;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Клиент внешнего источника рецептов. Сервисы и контроллеры зависят только от этого интерфейса.
 */
public interface RecipeSourceClient {

    /**
     * Ищет рецепты по названию.
     *
     * @param query текст запроса
     * @param limit максимальное число результатов
     */
    Flux<ExternalRecipeSearchItem> search(String query, int limit);

    /**
     * Загружает рецепт по идентификатору из результатов поиска.
     */
    Mono<ExternalRecipe> getById(String id);

    /**
     * Скачивает картинку; реализация сама проверяет, что адрес принадлежит её источнику.
     */
    Mono<ExternalImage> downloadImage(String imageUrl);

    /**
     * Название источника для логов и сообщений об ошибках.
     */
    String getSourceName();
}
