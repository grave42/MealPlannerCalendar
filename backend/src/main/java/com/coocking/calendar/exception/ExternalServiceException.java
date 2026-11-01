package com.coocking.calendar.exception;

/**
 * Внешний сервис недоступен или вернул неожиданный ответ (HTTP 502).
 */
public class ExternalServiceException extends CalendarException {

    public ExternalServiceException(String message, String userMessage) {
        super(message, userMessage);
    }

    public ExternalServiceException(String message, String userMessage, Throwable cause) {
        super(message, userMessage, cause);
    }
}
