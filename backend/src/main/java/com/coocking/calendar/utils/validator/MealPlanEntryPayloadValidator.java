package com.coocking.calendar.utils.validator;

import java.util.Objects;

import com.coocking.calendar.exception.ValidationException;
import com.coocking.calendar.model.request.MealPlanEntryRequestDto;
import org.springframework.stereotype.Component;

/**
 * Проверка обязательных полей записи плана питания.
 */
@Component
public class MealPlanEntryPayloadValidator implements DefaultPayloadValidator<MealPlanEntryRequestDto> {

    @Override
    public void validate(MealPlanEntryRequestDto payload) {
        if (Objects.isNull(payload.getDate())) {
            throw new ValidationException("Date is required", "Укажите дату");
        }
        if (Objects.isNull(payload.getMealType())) {
            throw new ValidationException("Meal type is required", "Укажите приём пищи");
        }
        if (Objects.isNull(payload.getDishId())) {
            throw new ValidationException("Dish id is required", "Выберите блюдо");
        }
    }
}
