package com.coocking.calendar.dao.converter;

import java.util.Set;
import java.util.stream.Collectors;

import com.coocking.calendar.dao.model.type.MealType;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.lang.NonNull;

/**
 * Преобразует набор меток приёмов пищи в JSONB-массив строк (в порядке объявления enum).
 */
@WritingConverter
public class MealTypeSetToJsonConverter implements Converter<Set<MealType>, Json> {

    @Override
    public Json convert(@NonNull Set<MealType> source) {
        String array = source.stream()
                .sorted()
                .map(type -> "\"" + type.name() + "\"")
                .collect(Collectors.joining(",", "[", "]"));
        return Json.of(array);
    }
}
