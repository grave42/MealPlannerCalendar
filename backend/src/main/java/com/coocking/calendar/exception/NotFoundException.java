package com.coocking.calendar.exception;

/**
 * Запрашиваемый объект не найден (HTTP 404).
 */
public class NotFoundException extends CalendarException {

    public NotFoundException(String message, String userMessage) {
        super(message, userMessage);
    }
}
