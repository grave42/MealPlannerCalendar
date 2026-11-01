package com.coocking.calendar.exception;

/**
 * Некорректный запрос (HTTP 400).
 */
public class BadRequestException extends CalendarException {

    public BadRequestException(String message, String userMessage) {
        super(message, userMessage);
    }
}
