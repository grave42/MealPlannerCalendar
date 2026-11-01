package com.coocking.calendar.controller;

import java.time.LocalDate;
import java.util.UUID;

import com.coocking.calendar.mapper.function.MealPlanEntryDetailsMapper;
import com.coocking.calendar.model.request.MealPlanEntryRequestDto;
import com.coocking.calendar.model.response.MealPlanEntryResponseDto;
import com.coocking.calendar.service.MealPlanService;
import com.coocking.calendar.utils.validator.MealPlanEntryPayloadValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * План питания.
 */
@Tag(name = "План питания", description = "Блюда по дням и приёмам пищи")
@RestController
@RequestMapping("/api/v1/meal-plan")
@RequiredArgsConstructor
public class MealPlanController {

    private final MealPlanService mealPlanService;
    private final MealPlanEntryDetailsMapper detailsMapper;
    private final MealPlanEntryPayloadValidator payloadValidator;

    @Operation(summary = "Записи плана за период", description = "Период включительно, не более 62 дней")
    @GetMapping
    public Flux<MealPlanEntryResponseDto> findInRange(
            @Parameter(description = "Начало периода", example = "2026-11-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Конец периода (включительно)", example = "2026-11-30")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return mealPlanService.findInRange(from, to)
                .map(detailsMapper::detailsToResponseDto);
    }

    @Operation(summary = "Добавить блюдо в слот",
            description = "422, если блюдо не подходит по меткам или уже есть в слоте")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<MealPlanEntryResponseDto> add(@RequestBody MealPlanEntryRequestDto request) {
        return Mono.fromRunnable(() -> payloadValidator.validate(request))
                .then(Mono.defer(() -> mealPlanService.addEntry(
                        request.getDate(), request.getMealType(), request.getDishId())))
                .map(detailsMapper::detailsToResponseDto);
    }

    @Operation(summary = "Убрать блюдо из слота")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable UUID id) {
        return mealPlanService.deleteEntry(id);
    }
}
