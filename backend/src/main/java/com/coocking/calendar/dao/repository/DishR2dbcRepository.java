package com.coocking.calendar.dao.repository;

import java.util.UUID;

import com.coocking.calendar.dao.model.entity.DishEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Репозиторий блюд.
 * <p>
 * Методы со словом {@code Summary} не выбирают колонку {@code image}: такие сущности нельзя сохранять.
 */
@Repository
public interface DishR2dbcRepository extends R2dbcRepository<DishEntity, UUID> {

    /**
     * Блюда для списка без картинки; {@code mealType} — имя метки или пустая строка (без фильтра),
     * {@code search} — шаблон для {@code LIKE} в нижнем регистре.
     */
    @Query("""
            SELECT id, name, description, ingredients, instructions, image_content_type, meal_types,
                   create_time, create_user, last_modify_time, last_modify_user
            FROM dish
            WHERE (:mealType = ''
                   OR meal_types @> jsonb_build_array(CAST(:mealType AS text))
                   OR meal_types = CAST('[]' AS jsonb))
              AND lower(name) LIKE :search ESCAPE '\\'
            ORDER BY lower(name), name
            """)
    Flux<DishEntity> findAllSummaries(String mealType, String search);

    /**
     * Блюдо без картинки по идентификатору.
     */
    @Query("""
            SELECT id, name, description, ingredients, instructions, image_content_type, meal_types,
                   create_time, create_user, last_modify_time, last_modify_user
            FROM dish
            WHERE id = :id
            """)
    Mono<DishEntity> findSummaryById(UUID id);

    /**
     * Только картинка блюда и служебные поля.
     */
    @Query("""
            SELECT id, image, image_content_type, last_modify_time
            FROM dish
            WHERE id = :id
            """)
    Mono<DishEntity> findImageById(UUID id);
}
