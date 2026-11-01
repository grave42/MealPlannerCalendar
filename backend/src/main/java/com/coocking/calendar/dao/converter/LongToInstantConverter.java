package com.coocking.calendar.dao.converter;

import java.time.Instant;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.lang.NonNull;

/**
 * Преобразует epoch millis из {@code bigint} в {@link Instant}.
 */
@ReadingConverter
public class LongToInstantConverter implements Converter<Long, Instant> {

    @Override
    public Instant convert(@NonNull Long source) {
        return Instant.ofEpochMilli(source);
    }
}
