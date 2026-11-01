package com.coocking.calendar.utils.validator;

import java.time.temporal.ChronoUnit;
import java.util.Objects;

import com.coocking.calendar.exception.BadRequestException;
import org.springframework.stereotype.Component;

/**
 * Проверка периода запроса плана питания: даты заданы, {@code from <= to}, не более 62 дней.
 */
@Component
public class DateRangeValidator implements DefaultPayloadValidator<DateRange> {

    /** Максимальное число дней в периоде (включительно) */
    public static final int MAX_DAYS = 62;

    @Override
    public void validate(DateRange payload) {
        if (Objects.isNull(payload.getFrom()) || Objects.isNull(payload.getTo())) {
            throw new BadRequestException("Both from and to are required", "Укажите начало и конец периода");
        }
        if (payload.getFrom().isAfter(payload.getTo())) {
            throw new BadRequestException("Parameter from must not be after to",
                    "Начало периода не может быть позже конца");
        }
        long days = ChronoUnit.DAYS.between(payload.getFrom(), payload.getTo()) + 1;
        if (days > MAX_DAYS) {
            throw new BadRequestException("Period must not exceed " + MAX_DAYS + " days",
                    "Период не может быть длиннее 62 дней");
        }
    }
}
