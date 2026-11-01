package com.coocking.calendar.exception;

/**
 * Данные не прошли проверку (HTTP 400).
 */
public class ValidationException extends CalendarException {

    public ValidationException(String message, String userMessage) {
        super(message, userMessage);
    }
}
