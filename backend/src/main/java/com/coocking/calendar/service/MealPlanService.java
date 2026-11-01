package com.coocking.calendar.service;

import java.time.LocalDate;
import java.util.UUID;

import com.coocking.calendar.dao.model.entity.DishEntity;
import com.coocking.calendar.dao.model.entity.MealPlanEntryEntity;
import com.coocking.calendar.dao.model.projection.MealPlanEntryDetails;
import com.coocking.calendar.dao.model.type.MealType;
import com.coocking.calendar.exception.ImpossibleOperationException;
import com.coocking.calendar.service.entity.DishService;
import com.coocking.calendar.service.entity.MealPlanEntryService;
import com.coocking.calendar.utils.validator.DateRange;
import com.coocking.calendar.utils.validator.DateRangeValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Составные операции над планом питания: проверка правил слота, дубликатов, назначение позиции.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MealPlanService {

    private final MealPlanEntryService entryService;
    private final DishService dishService;
    private final DateRangeValidator dateRangeValidator;

    /**
     * Записи плана за период (включительно, не более 62 дней).
     */
    public Flux<MealPlanEntryDetails> findInRange(LocalDate from, LocalDate to) {
        return Mono.deferContextual(Mono::just)
                .doOnNext(ctx -> dateRangeValidator.validate(new DateRange(from, to)))
                .flatMapMany(ctx -> entryService.findDetailsInRange(from, to));
    }

    /**
     * Добавляет блюдо в слот.
     *
     * @throws com.coocking.calendar.exception.NotFoundException            если блюда нет
     * @throws ImpossibleOperationException если блюдо не подходит слоту или уже в нём есть
     */
    @Transactional
    public Mono<MealPlanEntryDetails> addEntry(LocalDate date, MealType mealType, UUID dishId) {
        return Mono.deferContextual(Mono::just)
                .flatMap(ctx -> dishService.findSummaryById(dishId))
                .flatMap(dish -> checkSuitable(dish, mealType)
                        .then(checkNotDuplicate(date, mealType, dish))
                        .then(entryService.nextPosition(date, mealType))
                        .flatMap(position -> entryService.create(MealPlanEntryEntity.builder()
                                .planDate(date)
                                .mealType(mealType)
                                .dishId(dishId)
                                .position(position)
                                .build()))
                        .map(entry -> MealPlanEntryDetails.builder()
                                .id(entry.getId())
                                .planDate(entry.getPlanDate())
                                .mealType(entry.getMealType())
                                .position(entry.getPosition())
                                .dishId(dish.getId())
                                .dishName(dish.getName())
                                .hasImage(dish.hasImage())
                                .build()))
                .onErrorMap(DuplicateKeyException.class, e -> duplicate(date, mealType, dishId))
                .doOnSuccess(entry -> log.info("Блюдо добавлено в план: dishId={}, дата={}, приём пищи={}",
                        dishId, date, mealType))
                .doOnError(e -> log.warn("Не удалось добавить блюдо в план: dishId={}, дата={}, приём пищи={}: {}",
                        dishId, date, mealType, e.getMessage()));
    }

    /**
     * Убирает запись из плана.
     */
    @Transactional
    public Mono<Void> deleteEntry(UUID id) {
        return Mono.deferContextual(Mono::just)
                .flatMap(ctx -> entryService.deleteById(id))
                .doOnSuccess(v -> log.info("Запись плана удалена, id={}", id));
    }

    private Mono<Void> checkSuitable(DishEntity dish, MealType mealType) {
        boolean suitable = dish.getMealTypes() == null || dish.getMealTypes().isEmpty()
                || dish.getMealTypes().contains(mealType);
        if (suitable) {
            return Mono.empty();
        }
        return Mono.error(new ImpossibleOperationException(
                "Dish " + dish.getId() + " is not suitable for " + mealType,
                "Блюдо не подходит для выбранного приёма пищи"));
    }

    private Mono<Void> checkNotDuplicate(LocalDate date, MealType mealType, DishEntity dish) {
        return entryService.existsInSlot(date, mealType, dish.getId())
                .flatMap(exists -> Boolean.TRUE.equals(exists)
                        ? Mono.<Void>error(duplicate(date, mealType, dish.getId()))
                        : Mono.<Void>empty());
    }

    private ImpossibleOperationException duplicate(LocalDate date, MealType mealType, UUID dishId) {
        return new ImpossibleOperationException(
                "Dish " + dishId + " is already in slot " + date + "/" + mealType,
                "Это блюдо уже добавлено в выбранный приём пищи");
    }
}
