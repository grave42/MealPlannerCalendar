package com.coocking.calendar.utils.validator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.coocking.calendar.dao.model.type.MealType;
import com.coocking.calendar.exception.ValidationException;
import com.coocking.calendar.model.request.DishRequestDto;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Проверка данных блюда: название 1–200 символов, метки без дубликатов.
 */
@Component
public class DishPayloadValidator implements DefaultPayloadValidator<DishRequestDto> {

    private static final int MAX_NAME_LENGTH = 200;
    private static final int MAX_URL_LENGTH = 2048;

    @Override
    public void validate(DishRequestDto payload) {
        if (!StringUtils.hasText(payload.getName())) {
            throw new ValidationException("Dish name must not be blank", "Введите название блюда");
        }
        if (payload.getName().length() > MAX_NAME_LENGTH) {
            throw new ValidationException("Dish name is longer than " + MAX_NAME_LENGTH + " characters",
                    "Название не длиннее 200 символов");
        }
        validateMealTypes(payload.getMealTypes());
        if (payload.getSourceImageUrl() != null && payload.getSourceImageUrl().length() > MAX_URL_LENGTH) {
            throw new ValidationException("Source image url is too long", "Слишком длинный адрес картинки");
        }
    }

    private void validateMealTypes(List<MealType> mealTypes) {
        Set<MealType> seen = new HashSet<>();
        for (MealType mealType : mealTypes) {
            if (mealType == null) {
                throw new ValidationException("Meal type must not be null", "Некорректная метка приёма пищи");
            }
            if (!seen.add(mealType)) {
                throw new ValidationException("Duplicate meal type: " + mealType,
                        "Метки приёма пищи не должны повторяться");
            }
        }
    }
}
