package com.coocking.calendar.exception;

import lombok.Getter;

/**
 * Базовое исключение приложения.
 * <p>
 * {@code message} предназначено для логов и разработчика (английский),
 * {@code userMessage} показывается пользователю (русский).
 */
@Getter
public class CalendarException extends RuntimeException {

    /** Сообщение для пользователя на русском языке */
    private final String userMessage;

    public CalendarException(String message, String userMessage) {
        super(message);
        this.userMessage = userMessage;
    }

    public CalendarException(String message, String userMessage, Throwable cause) {
        super(message, cause);
        this.userMessage = userMessage;
    }
}
