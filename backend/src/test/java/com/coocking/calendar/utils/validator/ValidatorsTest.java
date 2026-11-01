package com.coocking.calendar.utils.validator;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import com.coocking.calendar.dao.model.type.MealType;
import com.coocking.calendar.exception.BadRequestException;
import com.coocking.calendar.exception.ValidationException;
import com.coocking.calendar.model.request.DishRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Валидаторы")
class ValidatorsTest {

    private DishRequestDto createDish(String name, List<MealType> mealTypes) {
        DishRequestDto dto = new DishRequestDto();
        dto.setName(name);
        dto.setMealTypes(mealTypes);
        return dto;
    }

    @Nested
    @DisplayName("Класс DishPayloadValidator")
    class DishPayload {

        private final DishPayloadValidator validator = new DishPayloadValidator();

        @Test
        @DisplayName("Корректное блюдо проходит")
        void testOk() {
            assertDoesNotThrow(() -> validator.validate(createDish("Омлет", List.of(MealType.BREAKFAST))));
            assertDoesNotThrow(() -> validator.validate(createDish("а".repeat(200), null)));
        }

        @Test
        @DisplayName("Пустое название отклоняется")
        void testBlankName() {
            assertThrows(ValidationException.class, () -> validator.validate(createDish(null, null)));
            assertThrows(ValidationException.class, () -> validator.validate(createDish("   ", null)));
        }

        @Test
        @DisplayName("Название длиннее 200 символов отклоняется")
        void testLongName() {
            assertThrows(ValidationException.class, () -> validator.validate(createDish("а".repeat(201), null)));
        }

        @Test
        @DisplayName("Дубликаты и null в метках отклоняются")
        void testMealTypes() {
            assertThrows(ValidationException.class,
                    () -> validator.validate(createDish("Омлет", List.of(MealType.LUNCH, MealType.LUNCH))));
            assertThrows(ValidationException.class,
                    () -> validator.validate(createDish("Омлет", Arrays.asList(MealType.LUNCH, null))));
        }
    }

    @Nested
    @DisplayName("Класс DateRangeValidator")
    class DateRangeCheck {

        private final DateRangeValidator validator = new DateRangeValidator();

        @Test
        @DisplayName("Один день и 62 дня допустимы")
        void testOk() {
            LocalDate from = LocalDate.of(2026, 1, 1);
            assertDoesNotThrow(() -> validator.validate(new DateRange(from, from)));
            assertDoesNotThrow(() -> validator.validate(new DateRange(from, from.plusDays(61))));
        }

        @Test
        @DisplayName("63 дня, from > to и пустые даты отклоняются")
        void testInvalid() {
            LocalDate from = LocalDate.of(2026, 1, 1);
            assertThrows(BadRequestException.class, () -> validator.validate(new DateRange(from, from.plusDays(62))));
            assertThrows(BadRequestException.class, () -> validator.validate(new DateRange(from, from.minusDays(1))));
            assertThrows(BadRequestException.class, () -> validator.validate(new DateRange(null, from)));
            assertThrows(BadRequestException.class, () -> validator.validate(new DateRange(from, null)));
        }
    }

    @Nested
    @DisplayName("Класс ExternalRecipeQueryValidator")
    class QueryCheck {

        private final ExternalRecipeQueryValidator validator = new ExternalRecipeQueryValidator();

        @Test
        @DisplayName("Запрос из 2 и более символов проходит, короче — нет")
        void testLength() {
            assertDoesNotThrow(() -> validator.validate("ab"));
            assertThrows(BadRequestException.class, () -> validator.validate("a"));
            assertThrows(BadRequestException.class, () -> validator.validate(" a "));
            assertThrows(BadRequestException.class, () -> validator.validate(null));
        }
    }
}
