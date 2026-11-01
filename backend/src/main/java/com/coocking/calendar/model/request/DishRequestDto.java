package com.coocking.calendar.model.request;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.coocking.calendar.dao.model.type.MealType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Запрос создания/обновления блюда (без картинки).
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Данные блюда для создания или обновления")
public class DishRequestDto implements RequestEntity {

    @Schema(description = "Название блюда", example = "Омлет", requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 1, maxLength = 200)
    private String name;

    @Schema(description = "Описание", example = "Нежный омлет на молоке")
    private String description;

    @Schema(description = "Состав для приготовления (многострочный текст)", example = "Яйца - 3 шт.\nМолоко - 100 мл")
    private String ingredients;

    @Schema(description = "Описание приготовления (многострочный текст)", example = "Взбейте яйца с молоком и обжарьте")
    private String instructions;

    @Schema(description = "Метки приёмов пищи; пустой набор — блюдо подходит для любого приёма пищи",
            example = "[\"BREAKFAST\", \"SNACK\"]")
    private List<MealType> mealTypes;

    @Schema(description = "Адрес картинки во внешнем источнике (используется только при создании блюда)",
            example = "https://img.iamcook.ru/2024/upl/recipes/zen/u-3c62.jpg")
    private String sourceImageUrl;

    /**
     * Метки блюда; если не переданы — пустой список.
     */
    public List<MealType> getMealTypes() {
        return Objects.isNull(mealTypes) ? new ArrayList<>() : mealTypes;
    }
}
