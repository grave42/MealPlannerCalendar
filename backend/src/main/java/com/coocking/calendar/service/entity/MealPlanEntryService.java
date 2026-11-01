package com.coocking.calendar.service.entity;

import java.time.LocalDate;
import java.util.UUID;

import com.coocking.calendar.dao.model.entity.MealPlanEntryEntity;
import com.coocking.calendar.dao.model.projection.MealPlanEntryDetails;
import com.coocking.calendar.dao.model.type.MealType;
import com.coocking.calendar.dao.repository.MealPlanEntryR2dbcRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Сервис записей плана питания.
 */
@Service
public class MealPlanEntryService extends BaseEntityService<MealPlanEntryEntity, UUID> {

    private final MealPlanEntryR2dbcRepository entryRepository;

    public MealPlanEntryService(MealPlanEntryR2dbcRepository entryRepository) {
        super(entryRepository, "Meal plan entry", "Запись плана питания не найдена");
        this.entryRepository = entryRepository;
    }

    /**
     * Записи за период (включительно) с краткими данными блюд.
     */
    public Flux<MealPlanEntryDetails> findDetailsInRange(LocalDate from, LocalDate to) {
        return Mono.deferContextual(Mono::just)
                .flatMapMany(ctx -> entryRepository.findDetailsInRange(from, to));
    }

    /**
     * Есть ли уже блюдо в слоте.
     */
    public Mono<Boolean> existsInSlot(LocalDate date, MealType mealType, UUID dishId) {
        return Mono.deferContextual(Mono::just)
                .flatMap(ctx -> entryRepository.existsByPlanDateAndMealTypeAndDishId(date, mealType, dishId));
    }

    /**
     * Позиция для нового блюда в слоте: последняя + 1.
     */
    public Mono<Integer> nextPosition(LocalDate date, MealType mealType) {
        return Mono.deferContextual(Mono::just)
                .flatMap(ctx -> entryRepository.findMaxPosition(date, mealType.name()))
                .map(max -> max + 1);
    }
}
