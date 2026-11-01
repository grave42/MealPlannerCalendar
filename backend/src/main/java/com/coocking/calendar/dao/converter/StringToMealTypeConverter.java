package com.coocking.calendar.dao.converter;

import com.coocking.calendar.dao.model.type.MealType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.lang.NonNull;

/**
 * Преобразует строку из БД в тип приёма пищи.
 */
@ReadingConverter
public class StringToMealTypeConverter implements Converter<String, MealType> {

    @Override
    public MealType convert(@NonNull String source) {
        return MealType.valueOf(source);
    }
}
