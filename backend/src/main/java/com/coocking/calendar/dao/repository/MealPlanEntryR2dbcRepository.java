package com.coocking.calendar.dao.repository;

import java.time.LocalDate;
import java.util.UUID;

import com.coocking.calendar.dao.model.entity.MealPlanEntryEntity;
import com.coocking.calendar.dao.model.projection.MealPlanEntryDetails;
import com.coocking.calendar.dao.model.type.MealType;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Репозиторий записей плана питания.
 */
@Repository
public interface MealPlanEntryR2dbcRepository extends R2dbcRepository<MealPlanEntryEntity, UUID> {

    /**
     * Записи за период (включительно) с краткими данными блюда без картинки.
     */
    @Query("""
            SELECT e.id AS id, e.plan_date AS plan_date, e.meal_type AS meal_type, e.position AS position,
                   d.id AS dish_id, d.name AS dish_name, (d.image_content_type IS NOT NULL) AS has_image
            FROM meal_plan_entry e
            JOIN dish d ON d.id = e.dish_id
            WHERE e.plan_date BETWEEN :from AND :to
            ORDER BY e.plan_date, e.meal_type, e.position
            """)
    Flux<MealPlanEntryDetails> findDetailsInRange(LocalDate from, LocalDate to);

    /**
     * Запись с краткими данными блюда по идентификатору.
     */
    @Query("""
            SELECT e.id AS id, e.plan_date AS plan_date, e.meal_type AS meal_type, e.position AS position,
                   d.id AS dish_id, d.name AS dish_name, (d.image_content_type IS NOT NULL) AS has_image
            FROM meal_plan_entry e
            JOIN dish d ON d.id = e.dish_id
            WHERE e.id = :id
            """)
    Mono<MealPlanEntryDetails> findDetailsById(UUID id);

    /**
     * Есть ли уже такое блюдо в слоте.
     */
    Mono<Boolean> existsByPlanDateAndMealTypeAndDishId(LocalDate planDate, MealType mealType, UUID dishId);

    /**
     * Максимальный номер позиции в слоте (0, если слот пуст).
     */
    @Query("""
            SELECT COALESCE(MAX(position), 0)
            FROM meal_plan_entry
            WHERE plan_date = :planDate AND meal_type = :mealType
            """)
    Mono<Integer> findMaxPosition(LocalDate planDate, String mealType);
}
