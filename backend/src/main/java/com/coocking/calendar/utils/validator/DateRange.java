package com.coocking.calendar.utils.validator;

import java.time.LocalDate;

import lombok.Value;

/**
 * Период дат (включительно).
 */
@Value
public class DateRange {

    LocalDate from;

    LocalDate to;
}
