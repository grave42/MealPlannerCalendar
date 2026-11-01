package com.coocking.calendar.exception;

/**
 * Операция невозможна в текущем состоянии данных (HTTP 422).
 */
public class ImpossibleOperationException extends CalendarException {

    public ImpossibleOperationException(String message, String userMessage) {
        super(message, userMessage);
    }
}
