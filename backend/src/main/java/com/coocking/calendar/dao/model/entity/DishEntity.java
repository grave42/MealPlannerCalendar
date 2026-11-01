package com.coocking.calendar.dao.model.entity;

import java.util.Objects;
import java.util.Set;

import com.coocking.calendar.dao.model.type.MealType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Блюдо из общей картотеки.
 * <p>
 * Запросы для списков не выбирают колонку {@code image}, поэтому такие экземпляры
 * нельзя сохранять обратно через {@code save}: картинка была бы затёрта.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table("dish")
public class DishEntity extends BaseEntity {

    /** Поле БД - name */
    @Column("name")
    private String name;

    /** Поле БД - description */
    @Column("description")
    private String description;

    /** Поле БД - ingredients */
    @Column("ingredients")
    private String ingredients;

    /** Поле БД - instructions */
    @Column("instructions")
    private String instructions;

    /** Поле БД - image */
    @Column("image")
    private byte[] image;

    /** Поле БД - image_content_type */
    @Column("image_content_type")
    private String imageContentType;

    /** Поле БД - meal_types (JSONB-массив строк) */
    @Column("meal_types")
    private Set<MealType> mealTypes;

    /**
     * Есть ли у блюда картинка (определяется по типу содержимого, чтобы не читать байты).
     */
    public boolean hasImage() {
        return Objects.nonNull(imageContentType);
    }
}
