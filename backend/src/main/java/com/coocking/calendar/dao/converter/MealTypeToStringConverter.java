package com.coocking.calendar.dao.converter;

import com.coocking.calendar.dao.model.type.MealType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.lang.NonNull;

/**
 * Преобразует тип приёма пищи в строку для БД.
 */
@WritingConverter
public class MealTypeToStringConverter implements Converter<MealType, String> {

    @Override
    public String convert(@NonNull MealType source) {
        return source.name();
    }
}
