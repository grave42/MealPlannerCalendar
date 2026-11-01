package com.coocking.calendar.dao.model.entity;

import java.time.LocalDate;
import java.util.UUID;

import com.coocking.calendar.dao.model.type.MealType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Запись плана питания: блюдо в слоте (дата + тип приёма пищи).
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table("meal_plan_entry")
public class MealPlanEntryEntity extends BaseEntity {

    /** Поле БД - plan_date */
    @Column("plan_date")
    private LocalDate planDate;

    /** Поле БД - meal_type */
    @Column("meal_type")
    private MealType mealType;

    /** Поле БД - dish_id */
    @Column("dish_id")
    private UUID dishId;

    /** Поле БД - position */
    @Column("position")
    private Integer position;
}
