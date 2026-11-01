package com.coocking.calendar.service.external;

import lombok.Builder;
import lombok.Value;

/**
 * Рецепт из внешнего источника.
 */
@Value
@Builder
public class ExternalRecipe {

    /** Идентификатор рецепта в источнике */
    String id;

    /** Название */
    String title;

    /** Описание (может отсутствовать) */
    String description;

    /** Состав, по строке на ингредиент (может отсутствовать) */
    String ingredients;

    /** Приготовление, по абзацу на шаг (может отсутствовать) */
    String instructions;

    /** Адрес картинки (может отсутствовать) */
    String imageUrl;
}
