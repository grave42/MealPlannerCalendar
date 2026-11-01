package com.coocking.calendar.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Рецепт из внешнего источника.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Рецепт из внешнего источника")
public class ExternalRecipeResponseDto implements ResponseEntity {

    @Schema(description = "Идентификатор рецепта в источнике", example = "37207")
    private String id;

    @Schema(description = "Название", example = "Паста аль-помодоро")
    private String title;

    @Schema(description = "Описание")
    private String description;

    @Schema(description = "Состав, по строке на ингредиент", example = "Паста - 450 г")
    private String ingredients;

    @Schema(description = "Приготовление, по абзацу на шаг")
    private String instructions;

    @Schema(description = "Адрес картинки (передаётся как sourceImageUrl при создании блюда)",
            example = "https://img.iamcook.ru/2024/upl/recipes/zen/u-3c62.jpg")
    private String imageUrl;
}
