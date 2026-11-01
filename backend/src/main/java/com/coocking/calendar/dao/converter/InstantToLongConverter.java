package com.coocking.calendar.dao.converter;

import java.time.Instant;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.lang.NonNull;

/**
 * Преобразует {@link Instant} в epoch millis для хранения в {@code bigint}.
 */
@WritingConverter
public class InstantToLongConverter implements Converter<Instant, Long> {

    @Override
    public Long convert(@NonNull Instant source) {
        return source.toEpochMilli();
    }
}
