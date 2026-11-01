package com.coocking.calendar.dao.converter;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import com.coocking.calendar.dao.model.type.MealType;
import io.r2dbc.postgresql.codec.Json;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Конвертеры БД")
class ConvertersTest {

    @Nested
    @DisplayName("MealType и строка")
    class MealTypeString {

        @Test
        @DisplayName("Туда и обратно")
        void testRoundTrip() {
            for (MealType type : MealType.values()) {
                String text = new MealTypeToStringConverter().convert(type);
                assertEquals(type.name(), text);
                assertEquals(type, new StringToMealTypeConverter().convert(text));
            }
        }

        @Test
        @DisplayName("Неизвестное значение — IllegalArgumentException")
        void testUnknown() {
            assertThrows(IllegalArgumentException.class, () -> new StringToMealTypeConverter().convert("BRUNCH"));
        }
    }

    @Nested
    @DisplayName("Набор меток и JSONB")
    class MealTypeSetJson {

        @Test
        @DisplayName("Запись: порядок по объявлению enum")
        void testWrite() {
            Json json = new MealTypeSetToJsonConverter().convert(Set.of(MealType.SNACK, MealType.BREAKFAST));

            assertEquals("[\"BREAKFAST\",\"SNACK\"]", json.asString());
        }

        @Test
        @DisplayName("Запись пустого набора")
        void testWriteEmpty() {
            assertEquals("[]", new MealTypeSetToJsonConverter().convert(Set.of()).asString());
        }

        @Test
        @DisplayName("Чтение")
        void testRead() {
            Set<MealType> result = new JsonToMealTypeSetConverter().convert(Json.of("[\"DINNER\", \"LUNCH\"]"));

            assertEquals(EnumSet.of(MealType.LUNCH, MealType.DINNER), result);
        }

        @Test
        @DisplayName("Чтение пустого массива")
        void testReadEmpty() {
            assertTrue(new JsonToMealTypeSetConverter().convert(Json.of("[]")).isEmpty());
        }

        @Test
        @DisplayName("Некорректный JSON — IllegalArgumentException")
        void testReadInvalid() {
            assertThrows(IllegalArgumentException.class, () -> new JsonToMealTypeSetConverter().convert(Json.of("{")));
        }
    }

    @Nested
    @DisplayName("Instant и bigint")
    class InstantLong {

        @Test
        @DisplayName("Туда и обратно с точностью до миллисекунды")
        void testRoundTrip() {
            Instant instant = Instant.parse("2026-11-01T08:15:30.123Z");

            Long millis = new InstantToLongConverter().convert(instant);

            assertEquals(instant.toEpochMilli(), millis);
            assertEquals(instant, new LongToInstantConverter().convert(millis));
        }
    }
}
