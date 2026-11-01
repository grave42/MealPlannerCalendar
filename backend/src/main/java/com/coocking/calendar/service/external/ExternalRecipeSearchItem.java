package com.coocking.calendar.service.external;

import lombok.Builder;
import lombok.Value;

/**
 * Элемент результатов поиска во внешнем источнике.
 */
@Value
@Builder
public class ExternalRecipeSearchItem {

    /** Идентификатор рецепта в источнике */
    String id;

    /** Название */
    String title;

    /** Адрес миниатюры (может отсутствовать) */
    String imageUrl;
}
