package com.coocking.calendar.dao.converter;

import java.util.EnumSet;
import java.util.Set;

import com.coocking.calendar.dao.model.type.MealType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.lang.NonNull;

/**
 * Преобразует JSONB-массив строк из БД в набор меток приёмов пищи.
 */
@ReadingConverter
public class JsonToMealTypeSetConverter implements Converter<Json, Set<MealType>> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public Set<MealType> convert(@NonNull Json source) {
        Set<MealType> result = EnumSet.noneOf(MealType.class);
        try {
            for (String name : OBJECT_MAPPER.readValue(source.asString(), String[].class)) {
                result.add(MealType.valueOf(name));
            }
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid meal types JSON: " + source.asString(), e);
        }
        return result;
    }
}
