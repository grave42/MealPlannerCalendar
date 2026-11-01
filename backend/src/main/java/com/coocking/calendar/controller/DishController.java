package com.coocking.calendar.controller;

import java.util.UUID;

import com.coocking.calendar.dao.model.type.MealType;
import com.coocking.calendar.mapper.entity.DishEntityMapper;
import com.coocking.calendar.model.request.DishRequestDto;
import com.coocking.calendar.model.response.DishResponseDto;
import com.coocking.calendar.service.entity.DishService;
import com.coocking.calendar.utils.validator.DishPayloadValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Картотека блюд.
 */
@Tag(name = "Блюда", description = "Картотека блюд: создание, редактирование, картинки")
@RestController
@RequestMapping("/api/v1/dish")
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;
    private final DishEntityMapper dishMapper;
    private final DishPayloadValidator dishValidator;

    @Operation(summary = "Создать блюдо",
            description = "Если передан sourceImageUrl, картинка скачивается из внешнего источника; "
                    + "при сбое блюдо создаётся без картинки")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<DishResponseDto> create(@RequestBody DishRequestDto request) {
        return Mono.fromSupplier(() -> {
                    dishValidator.validate(request);
                    return dishMapper.requestDtoToEntity(request);
                })
                .flatMap(entity -> dishService.create(entity, request.getSourceImageUrl()))
                .map(dishMapper::entityToResponseDto);
    }

    @Operation(summary = "Список блюд",
            description = "С mealType возвращаются блюда с этой меткой и все блюда без меток; "
                    + "search — поиск по подстроке названия без учёта регистра. Сортировка по названию")
    @GetMapping
    public Flux<DishResponseDto> findAll(@RequestParam(required = false) MealType mealType,
                                         @RequestParam(required = false) String search) {
        return dishService.findAll(mealType, search)
                .map(dishMapper::entityToResponseDto);
    }

    @Operation(summary = "Получить блюдо")
    @GetMapping("/{id}")
    public Mono<DishResponseDto> findById(@PathVariable UUID id) {
        return dishService.findSummaryById(id)
                .map(dishMapper::entityToResponseDto);
    }

    @Operation(summary = "Обновить блюдо", description = "Картинка не изменяется")
    @PutMapping("/{id}")
    public Mono<DishResponseDto> update(@PathVariable UUID id, @RequestBody DishRequestDto request) {
        return Mono.fromSupplier(() -> {
                    dishValidator.validate(request);
                    return dishMapper.requestDtoToEntity(request);
                })
                .flatMap(patch -> dishService.update(id, patch))
                .map(dishMapper::entityToResponseDto);
    }

    @Operation(summary = "Удалить блюдо", description = "Записи плана питания с этим блюдом удаляются каскадно")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable UUID id) {
        return dishService.deleteById(id);
    }

    @Operation(summary = "Загрузить или заменить картинку",
            description = "multipart/form-data, часть file; JPEG, PNG или WEBP до 5 МБ")
    @PutMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<DishResponseDto> uploadImage(@PathVariable UUID id, @RequestPart("file") Mono<FilePart> file) {
        return file
                .flatMap(part -> dishService.setImage(id, part.content()))
                .map(dishMapper::entityToResponseDto);
    }

    @Operation(summary = "Удалить картинку")
    @DeleteMapping("/{id}/image")
    public Mono<DishResponseDto> deleteImage(@PathVariable UUID id) {
        return dishService.removeImage(id)
                .map(dishMapper::entityToResponseDto);
    }

    @Operation(summary = "Получить картинку", description = "Байты картинки; 404, если картинки нет")
    @GetMapping("/{id}/image")
    public Mono<ResponseEntity<byte[]>> getImage(@PathVariable UUID id) {
        return dishService.findImage(id)
                .map(dish -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(dish.getImageContentType()))
                        .cacheControl(CacheControl.noCache())
                        .header(HttpHeaders.ETAG, "\"" + dish.getLastModifyTime().toEpochMilli() + "\"")
                        .body(dish.getImage()));
    }
}
