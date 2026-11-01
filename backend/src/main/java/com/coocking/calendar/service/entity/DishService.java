package com.coocking.calendar.service.entity;

import java.util.Objects;
import java.util.UUID;

import com.coocking.calendar.dao.model.entity.DishEntity;
import com.coocking.calendar.dao.model.type.MealType;
import com.coocking.calendar.dao.repository.DishR2dbcRepository;
import com.coocking.calendar.exception.NotFoundException;
import com.coocking.calendar.mapper.entity.DishEntityMapper;
import com.coocking.calendar.service.ImageService;
import com.coocking.calendar.service.external.RecipeSourceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Сервис блюд: CRUD, выбор по правилу меток, работа с картинкой.
 */
@Slf4j
@Service
public class DishService extends BaseEntityService<DishEntity, UUID> {

    private final DishR2dbcRepository dishRepository;
    private final DishEntityMapper dishMapper;
    private final RecipeSourceClient recipeSourceClient;
    private final ImageService imageService;

    public DishService(DishR2dbcRepository dishRepository, DishEntityMapper dishMapper,
                       RecipeSourceClient recipeSourceClient, ImageService imageService) {
        super(dishRepository, "Dish", "Блюдо не найдено");
        this.dishRepository = dishRepository;
        this.dishMapper = dishMapper;
        this.recipeSourceClient = recipeSourceClient;
        this.imageService = imageService;
    }

    /**
     * Создаёт блюдо; если задан {@code sourceImageUrl}, скачивает картинку из внешнего источника.
     * Если скачать картинку не удалось, блюдо создаётся без картинки.
     */
    @Transactional
    public Mono<DishEntity> create(DishEntity dish, String sourceImageUrl) {
        return Mono.deferContextual(Mono::just)
                .flatMap(ctx -> attachSourceImage(dish, sourceImageUrl))
                .flatMap(super::create);
    }

    /**
     * Обновляет поля блюда (картинка не затрагивается).
     */
    @Transactional
    public Mono<DishEntity> update(UUID id, DishEntity patch) {
        return Mono.deferContextual(Mono::just)
                .flatMap(ctx -> findById(id))
                .map(existing -> {
                    dishMapper.patchEntityFromEntity(patch, existing);
                    return existing;
                })
                .flatMap(super::update);
    }

    /**
     * Блюда по правилу предложения: с меткой {@code mealType} и все без меток; фильтр по подстроке названия.
     * Без {@code mealType} возвращаются все блюда. Результат без картинок.
     */
    public Flux<DishEntity> findAll(MealType mealType, String search) {
        String typeFilter = Objects.isNull(mealType) ? "" : mealType.name();
        return Mono.deferContextual(Mono::just)
                .flatMapMany(ctx -> dishRepository.findAllSummaries(typeFilter, buildLikePattern(search)));
    }

    /**
     * Блюдо без картинки (такую сущность нельзя сохранять).
     */
    public Mono<DishEntity> findSummaryById(UUID id) {
        return Mono.deferContextual(Mono::just)
                .flatMap(ctx -> dishRepository.findSummaryById(id))
                .switchIfEmpty(Mono.error(() -> notFound(id)));
    }

    /**
     * Блюдо с байтами картинки; ошибка, если картинки нет.
     */
    public Mono<DishEntity> findImage(UUID id) {
        return Mono.deferContextual(Mono::just)
                .flatMap(ctx -> dishRepository.findImageById(id))
                .switchIfEmpty(Mono.error(() -> notFound(id)))
                .flatMap(dish -> Objects.isNull(dish.getImage())
                        ? Mono.<DishEntity>error(new NotFoundException("Dish image not found: " + id,
                        "У блюда нет картинки"))
                        : Mono.just(dish));
    }

    /**
     * Загружает или заменяет картинку блюда.
     */
    @Transactional
    public Mono<DishEntity> setImage(UUID id, Flux<DataBuffer> content) {
        return Mono.deferContextual(Mono::just)
                .flatMap(ctx -> imageService.readBytes(content))
                .flatMap(bytes -> findById(id).map(dish -> {
                    dish.setImageContentType(imageService.detectContentType(bytes));
                    dish.setImage(bytes);
                    return dish;
                }))
                .flatMap(super::update)
                .doOnSuccess(dish -> log.info("Картинка блюда сохранена, dishId={}", id));
    }

    /**
     * Удаляет картинку блюда.
     */
    @Transactional
    public Mono<DishEntity> removeImage(UUID id) {
        return Mono.deferContextual(Mono::just)
                .flatMap(ctx -> findById(id))
                .map(dish -> {
                    dish.setImage(null);
                    dish.setImageContentType(null);
                    return dish;
                })
                .flatMap(super::update)
                .doOnSuccess(dish -> log.info("Картинка блюда удалена, dishId={}", id));
    }

    private Mono<DishEntity> attachSourceImage(DishEntity dish, String sourceImageUrl) {
        if (!StringUtils.hasText(sourceImageUrl)) {
            return Mono.just(dish);
        }
        return recipeSourceClient.downloadImage(sourceImageUrl)
                .map(image -> {
                    dish.setImageContentType(imageService.detectContentType(image.getContent()));
                    dish.setImage(image.getContent());
                    return dish;
                })
                .onErrorResume(e -> {
                    log.warn("Не удалось скачать картинку из источника {}, блюдо '{}' создаётся без картинки: {}",
                            recipeSourceClient.getSourceName(), dish.getName(), e.getMessage());
                    return Mono.just(dish);
                })
                .defaultIfEmpty(dish);
    }

    private String buildLikePattern(String search) {
        if (!StringUtils.hasText(search)) {
            return "%";
        }
        String escaped = search.trim().toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
