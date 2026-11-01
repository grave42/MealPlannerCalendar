package com.coocking.calendar.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Элемент результатов поиска рецептов во внешнем источнике.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Найденный во внешнем источнике рецепт")
public class ExternalRecipeSearchItemResponseDto implements ResponseEntity {

    @Schema(description = "Идентификатор рецепта в источнике", example = "37207")
    private String id;

    @Schema(description = "Название", example = "Паста аль-помодоро")
    private String title;

    @Schema(description = "Адрес миниатюры", example = "https://img.iamcook.ru/2024/upl/recipes/middle/u-3c62.jpg")
    private String imageUrl;
}
