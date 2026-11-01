package com.coocking.calendar.dao.model.projection;

import java.time.LocalDate;
import java.util.UUID;

import com.coocking.calendar.dao.model.type.MealType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Запись плана питания вместе с краткими данными блюда (без картинки).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanEntryDetails {

    /** Идентификатор записи плана */
    private UUID id;

    /** Дата */
    private LocalDate planDate;

    /** Тип приёма пищи */
    private MealType mealType;

    /** Порядковый номер в слоте */
    private Integer position;

    /** Идентификатор блюда */
    private UUID dishId;

    /** Название блюда */
    private String dishName;

    /** Есть ли у блюда картинка */
    private Boolean hasImage;
}
