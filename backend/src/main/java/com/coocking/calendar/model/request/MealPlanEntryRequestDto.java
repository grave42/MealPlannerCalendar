package com.coocking.calendar.model.request;

import java.time.LocalDate;
import java.util.UUID;

import com.coocking.calendar.dao.model.type.MealType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Запрос добавления блюда в слот плана питания.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Добавление блюда в слот плана питания")
public class MealPlanEntryRequestDto implements RequestEntity {

    @Schema(description = "Дата", example = "2026-11-15", format = "date",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate date;

    @Schema(description = "Тип приёма пищи", example = "BREAKFAST", requiredMode = Schema.RequiredMode.REQUIRED)
    private MealType mealType;

    @Schema(description = "Идентификатор блюда", example = "0b8f1c3e-5d1a-4c71-9a35-0f7a7a1f2c10",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID dishId;
}
