package com.coocking.calendar.controller;

import java.time.LocalDate;
import java.util.UUID;

import com.coocking.calendar.dao.model.projection.MealPlanEntryDetails;
import com.coocking.calendar.dao.model.type.MealType;
import com.coocking.calendar.exception.BadRequestException;
import com.coocking.calendar.exception.ImpossibleOperationException;
import com.coocking.calendar.exception.NotFoundException;
import com.coocking.calendar.exception.handler.GlobalExceptionHandler;
import com.coocking.calendar.mapper.function.MealPlanEntryDetailsMapperImpl;
import com.coocking.calendar.service.MealPlanService;
import com.coocking.calendar.utils.validator.MealPlanEntryPayloadValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Класс MealPlanController")
@WebFluxTest(MealPlanController.class)
@ContextConfiguration(classes = {MealPlanController.class, MealPlanEntryDetailsMapperImpl.class,
        MealPlanEntryPayloadValidator.class, GlobalExceptionHandler.class})
class MealPlanControllerTest {

    private static final UUID DISH_ID = UUID.fromString("0b8f1c3e-5d1a-4c71-9a35-0f7a7a1f2c10");
    private static final UUID ENTRY_ID = UUID.fromString("5f2c9d58-1b7e-4a53-8f55-3c1d1c4a9a11");

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private MealPlanService mealPlanService;

    private MealPlanEntryDetails createTestDetails(int position) {
        return MealPlanEntryDetails.builder()
                .id(ENTRY_ID).planDate(LocalDate.of(2026, 11, 15)).mealType(MealType.LUNCH)
                .position(position).dishId(DISH_ID).dishName("Суп").hasImage(false).build();
    }

    private String body(String mealType) {
        return "{\"date\":\"2026-11-15\",\"mealType\":\"" + mealType + "\",\"dishId\":\"" + DISH_ID + "\"}";
    }

    @Nested
    @DisplayName("Записи за период")
    class FindInRange {

        @Test
        @DisplayName("Возвращаются записи с краткими данными блюда")
        void testOk() {
            when(mealPlanService.findInRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)))
                    .thenReturn(Flux.just(createTestDetails(1), createTestDetails(2)));

            webTestClient.get().uri("/api/v1/meal-plan?from=2026-11-01&to=2026-11-30").exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.length()").isEqualTo(2)
                    .jsonPath("$[0].date").isEqualTo("2026-11-15")
                    .jsonPath("$[0].mealType").isEqualTo("LUNCH")
                    .jsonPath("$[0].dishId").isEqualTo(DISH_ID.toString())
                    .jsonPath("$[0].dishName").isEqualTo("Суп")
                    .jsonPath("$[0].hasImage").isEqualTo(false)
                    .jsonPath("$[1].position").isEqualTo(2);
        }

        @Test
        @DisplayName("Слишком длинный период даёт 400 с userMessage")
        void testTooLong() {
            when(mealPlanService.findInRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                    .thenReturn(Flux.error(new BadRequestException("Period must not exceed 62 days",
                            "Период не может быть длиннее 62 дней")));

            webTestClient.get().uri("/api/v1/meal-plan?from=2026-01-01&to=2026-12-31").exchange()
                    .expectStatus().isBadRequest()
                    .expectBody().jsonPath("$.userMessage").isEqualTo("Период не может быть длиннее 62 дней");
        }

        @Test
        @DisplayName("Некорректная дата или отсутствующий параметр дают 400")
        void testInvalidDates() {
            webTestClient.get().uri("/api/v1/meal-plan?from=abc&to=2026-11-30").exchange()
                    .expectStatus().isBadRequest();
            webTestClient.get().uri("/api/v1/meal-plan?from=2026-11-01").exchange()
                    .expectStatus().isBadRequest();
        }
    }

    @Nested
    @DisplayName("Добавление блюда в слот")
    class Add {

        @Test
        @DisplayName("Успешное добавление возвращает 201")
        void testOk() {
            when(mealPlanService.addEntry(LocalDate.of(2026, 11, 15), MealType.LUNCH, DISH_ID))
                    .thenReturn(Mono.just(createTestDetails(1)));

            webTestClient.post().uri("/api/v1/meal-plan").contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body("LUNCH")).exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.id").isEqualTo(ENTRY_ID.toString())
                    .jsonPath("$.position").isEqualTo(1);
        }

        @Test
        @DisplayName("Неподходящее блюдо или дубликат дают 422 с userMessage")
        void testImpossible() {
            when(mealPlanService.addEntry(any(), any(), any())).thenReturn(Mono.error(
                    new ImpossibleOperationException("Not suitable", "Блюдо не подходит для выбранного приёма пищи")));

            webTestClient.post().uri("/api/v1/meal-plan").contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body("LUNCH")).exchange()
                    .expectStatus().isEqualTo(422)
                    .expectBody().jsonPath("$.userMessage").isEqualTo("Блюдо не подходит для выбранного приёма пищи");
        }

        @Test
        @DisplayName("Несуществующее блюдо даёт 404")
        void testDishNotFound() {
            when(mealPlanService.addEntry(any(), any(), any()))
                    .thenReturn(Mono.error(new NotFoundException("Dish not found", "Блюдо не найдено")));

            webTestClient.post().uri("/api/v1/meal-plan").contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body("LUNCH")).exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        @DisplayName("Отсутствие обязательных полей даёт 400")
        void testMissingFields() {
            webTestClient.post().uri("/api/v1/meal-plan").contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"date\":\"2026-11-15\"}").exchange()
                    .expectStatus().isBadRequest()
                    .expectBody().jsonPath("$.userMessage").isEqualTo("Укажите приём пищи");

            verify(mealPlanService, never()).addEntry(any(), any(), any());
        }

        @Test
        @DisplayName("Неизвестный тип приёма пищи даёт 400")
        void testUnknownMealType() {
            webTestClient.post().uri("/api/v1/meal-plan").contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body("BRUNCH")).exchange()
                    .expectStatus().isBadRequest();
        }
    }

    @Nested
    @DisplayName("Удаление записи")
    class Delete {

        @Test
        @DisplayName("Существующая запись удаляется")
        void testOk() {
            when(mealPlanService.deleteEntry(ENTRY_ID)).thenReturn(Mono.empty());

            webTestClient.delete().uri("/api/v1/meal-plan/" + ENTRY_ID).exchange()
                    .expectStatus().isNoContent();
        }

        @Test
        @DisplayName("Несуществующая запись даёт 404")
        void testNotFound() {
            when(mealPlanService.deleteEntry(ENTRY_ID))
                    .thenReturn(Mono.error(new NotFoundException("Entry not found", "Запись не найдена")));

            webTestClient.delete().uri("/api/v1/meal-plan/" + ENTRY_ID).exchange()
                    .expectStatus().isNotFound();
        }
    }
}
