package com.coocking.calendar.utils.validator;

/**
 * Валидатор входных данных.
 *
 * @param <T> тип проверяемых данных
 */
public interface DefaultPayloadValidator<T> {

    /**
     * Проверяет данные.
     *
     * @throws com.coocking.calendar.exception.ValidationException если данные некорректны
     */
    void validate(T payload);
}
