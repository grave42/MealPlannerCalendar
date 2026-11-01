package com.coocking.calendar.service;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import com.coocking.calendar.dao.model.entity.DishEntity;
import com.coocking.calendar.dao.model.entity.MealPlanEntryEntity;
import com.coocking.calendar.dao.model.projection.MealPlanEntryDetails;
import com.coocking.calendar.dao.model.type.MealType;
import com.coocking.calendar.exception.BadRequestException;
import com.coocking.calendar.exception.ImpossibleOperationException;
import com.coocking.calendar.exception.NotFoundException;
import com.coocking.calendar.service.entity.DishService;
import com.coocking.calendar.service.entity.MealPlanEntryService;
import com.coocking.calendar.utils.validator.DateRangeValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Класс MealPlanService")
@SpringJUnitConfig(classes = {MealPlanService.class, DateRangeValidator.class})
class MealPlanServiceTest {

    private static final UUID DISH_ID = UUID.fromString("0b8f1c3e-5d1a-4c71-9a35-0f7a7a1f2c10");
    private static final UUID ENTRY_ID = UUID.fromString("5f2c9d58-1b7e-4a53-8f55-3c1d1c4a9a11");
    private static final LocalDate DATE = LocalDate.of(2026, 11, 15);

    @Autowired
    private MealPlanService mealPlanService;

    @MockitoBean
    private MealPlanEntryService entryService;

    @MockitoBean
    private DishService dishService;

    private DishEntity createTestDish(Set<MealType> mealTypes) {
        return DishEntity.builder().id(DISH_ID).name("Омлет").mealTypes(mealTypes).imageContentType("image/png").build();
    }

    private void mockCreateWithId() {
        when(entryService.create(any(MealPlanEntryEntity.class))).thenAnswer(i -> {
            MealPlanEntryEntity entity = i.getArgument(0);
            entity.setId(ENTRY_ID);
            return Mono.just(entity);
        });
    }

    @Nested
    @DisplayName("Добавление блюда в слот")
    class AddEntry {

        @Test
        @DisplayName("Подходящее по метке блюдо добавляется с позицией «последний + 1»")
        void testOk() {
            when(dishService.findSummaryById(DISH_ID)).thenReturn(Mono.just(createTestDish(Set.of(MealType.BREAKFAST))));
            when(entryService.existsInSlot(DATE, MealType.BREAKFAST, DISH_ID)).thenReturn(Mono.just(false));
            when(entryService.nextPosition(DATE, MealType.BREAKFAST)).thenReturn(Mono.just(3));
            mockCreateWithId();

            StepVerifier.create(mealPlanService.addEntry(DATE, MealType.BREAKFAST, DISH_ID))
                    .assertNext(details -> {
                        assertEquals(ENTRY_ID, details.getId());
                        assertEquals(3, details.getPosition());
                        assertEquals("Омлет", details.getDishName());
                        assertEquals(true, details.getHasImage());
                        assertEquals(DATE, details.getPlanDate());
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Блюдо без меток подходит любому слоту")
        void testDishWithoutMealTypes() {
            when(dishService.findSummaryById(DISH_ID)).thenReturn(Mono.just(createTestDish(Set.of())));
            when(entryService.existsInSlot(DATE, MealType.LUNCH, DISH_ID)).thenReturn(Mono.just(false));
            when(entryService.nextPosition(DATE, MealType.LUNCH)).thenReturn(Mono.just(1));
            mockCreateWithId();

            StepVerifier.create(mealPlanService.addEntry(DATE, MealType.LUNCH, DISH_ID))
                    .assertNext(details -> assertEquals(1, details.getPosition()))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Блюдо с другой меткой — ImpossibleOperationException, запись не создаётся")
        void testUnsuitableDish() {
            when(dishService.findSummaryById(DISH_ID)).thenReturn(Mono.just(createTestDish(Set.of(MealType.BREAKFAST))));

            StepVerifier.create(mealPlanService.addEntry(DATE, MealType.LUNCH, DISH_ID))
                    .expectError(ImpossibleOperationException.class)
                    .verify();

            verify(entryService, never()).create(any(MealPlanEntryEntity.class));
        }

        @Test
        @DisplayName("Дубликат в слоте — ImpossibleOperationException")
        void testDuplicate() {
            when(dishService.findSummaryById(DISH_ID)).thenReturn(Mono.just(createTestDish(Set.of())));
            when(entryService.existsInSlot(DATE, MealType.DINNER, DISH_ID)).thenReturn(Mono.just(true));

            StepVerifier.create(mealPlanService.addEntry(DATE, MealType.DINNER, DISH_ID))
                    .expectError(ImpossibleOperationException.class)
                    .verify();

            verify(entryService, never()).create(any(MealPlanEntryEntity.class));
        }

        @Test
        @DisplayName("Гонка: нарушение уникального индекса превращается в ImpossibleOperationException")
        void testDuplicateKey() {
            when(dishService.findSummaryById(DISH_ID)).thenReturn(Mono.just(createTestDish(Set.of())));
            when(entryService.existsInSlot(DATE, MealType.DINNER, DISH_ID)).thenReturn(Mono.just(false));
            when(entryService.nextPosition(DATE, MealType.DINNER)).thenReturn(Mono.just(1));
            when(entryService.create(any(MealPlanEntryEntity.class)))
                    .thenReturn(Mono.error(new DuplicateKeyException("duplicate")));

            StepVerifier.create(mealPlanService.addEntry(DATE, MealType.DINNER, DISH_ID))
                    .expectError(ImpossibleOperationException.class)
                    .verify();
        }

        @Test
        @DisplayName("Несуществующее блюдо — NotFoundException")
        void testDishNotFound() {
            when(dishService.findSummaryById(DISH_ID))
                    .thenReturn(Mono.error(new NotFoundException("Dish not found", "Блюдо не найдено")));

            StepVerifier.create(mealPlanService.addEntry(DATE, MealType.DINNER, DISH_ID))
                    .expectError(NotFoundException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("Записи за период")
    class FindInRange {

        @Test
        @DisplayName("Период в пределах 62 дней")
        void testOk() {
            LocalDate from = LocalDate.of(2026, 11, 1);
            LocalDate to = LocalDate.of(2026, 11, 30);
            MealPlanEntryDetails details = MealPlanEntryDetails.builder().id(ENTRY_ID).planDate(DATE).build();
            when(entryService.findDetailsInRange(from, to)).thenReturn(Flux.just(details));

            StepVerifier.create(mealPlanService.findInRange(from, to)).expectNext(details).verifyComplete();
        }

        @Test
        @DisplayName("Ровно 62 дня допустимы")
        void testMaxDays() {
            LocalDate from = LocalDate.of(2026, 1, 1);
            when(entryService.findDetailsInRange(any(), any())).thenReturn(Flux.empty());

            StepVerifier.create(mealPlanService.findInRange(from, from.plusDays(61))).verifyComplete();
        }

        @Test
        @DisplayName("Больше 62 дней — BadRequestException")
        void testTooLong() {
            LocalDate from = LocalDate.of(2026, 1, 1);

            StepVerifier.create(mealPlanService.findInRange(from, LocalDate.of(2026, 12, 31)))
                    .expectError(BadRequestException.class)
                    .verify();

            verify(entryService, never()).findDetailsInRange(any(), any());
        }

        @Test
        @DisplayName("from позже to — BadRequestException")
        void testFromAfterTo() {
            StepVerifier.create(mealPlanService.findInRange(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1)))
                    .expectError(BadRequestException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("Удаление записи")
    class DeleteEntry {

        @Test
        @DisplayName("Существующая запись удаляется")
        void testOk() {
            when(entryService.deleteById(ENTRY_ID)).thenReturn(Mono.empty());

            StepVerifier.create(mealPlanService.deleteEntry(ENTRY_ID)).verifyComplete();
        }

        @Test
        @DisplayName("Несуществующая запись — NotFoundException")
        void testNotFound() {
            when(entryService.deleteById(ENTRY_ID))
                    .thenReturn(Mono.error(new NotFoundException("Entry not found", "Запись не найдена")));

            StepVerifier.create(mealPlanService.deleteEntry(ENTRY_ID)).expectError(NotFoundException.class).verify();
        }
    }
}
