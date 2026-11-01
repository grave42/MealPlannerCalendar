package com.coocking.calendar.model.response;

import java.time.LocalDate;
import java.util.UUID;

import com.coocking.calendar.dao.model.type.MealType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Запись плана питания с краткими данными блюда.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запись плана питания")
public class MealPlanEntryResponseDto implements ResponseEntity {

    @Schema(description = "Идентификатор записи", example = "5f2c9d58-1b7e-4a53-8f55-3c1d1c4a9a11")
    private UUID id;

    @Schema(description = "Дата", example = "2026-11-15", format = "date")
    private LocalDate date;

    @Schema(description = "Тип приёма пищи", example = "BREAKFAST")
    private MealType mealType;

    @Schema(description = "Порядковый номер в слоте", example = "1")
    private Integer position;

    @Schema(description = "Идентификатор блюда", example = "0b8f1c3e-5d1a-4c71-9a35-0f7a7a1f2c10")
    private UUID dishId;

    @Schema(description = "Название блюда", example = "Омлет")
    private String dishName;

    @Schema(description = "Есть ли у блюда картинка", example = "true")
    private Boolean hasImage;
}
