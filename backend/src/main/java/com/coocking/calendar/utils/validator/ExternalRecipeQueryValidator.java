package com.coocking.calendar.utils.validator;

import com.coocking.calendar.exception.BadRequestException;
import org.springframework.stereotype.Component;

/**
 * Проверка поискового запроса к внешнему источнику рецептов: не короче 2 символов.
 */
@Component
public class ExternalRecipeQueryValidator implements DefaultPayloadValidator<String> {

    /** Минимальная длина запроса */
    public static final int MIN_QUERY_LENGTH = 2;

    @Override
    public void validate(String payload) {
        if (payload == null || payload.trim().length() < MIN_QUERY_LENGTH) {
            throw new BadRequestException("Query must be at least " + MIN_QUERY_LENGTH + " characters long",
                    "Введите не менее 2 символов для поиска");
        }
    }
}
