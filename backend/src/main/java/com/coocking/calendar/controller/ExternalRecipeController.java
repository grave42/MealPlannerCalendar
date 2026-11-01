package com.coocking.calendar.controller;

import com.coocking.calendar.exception.BadRequestException;
import com.coocking.calendar.mapper.function.ExternalRecipeMapper;
import com.coocking.calendar.model.response.ExternalRecipeResponseDto;
import com.coocking.calendar.model.response.ExternalRecipeSearchItemResponseDto;
import com.coocking.calendar.service.ExternalRecipeService;
import com.coocking.calendar.utils.validator.ExternalRecipeQueryValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Поиск и загрузка рецептов из внешнего источника.
 */
@Tag(name = "Внешние рецепты", description = "Поиск рецептов во внешнем источнике (сейчас iamcook.ru)")
@RestController
@RequestMapping("/api/v1/external-recipe")
@RequiredArgsConstructor
public class ExternalRecipeController {

    private final ExternalRecipeService externalRecipeService;
    private final ExternalRecipeMapper recipeMapper;
    private final ExternalRecipeQueryValidator queryValidator;

    @Operation(summary = "Поиск рецептов", description = "Не более 10 результатов; query не короче 2 символов; "
            + "502, если источник недоступен")
    @GetMapping("/search")
    public Flux<ExternalRecipeSearchItemResponseDto> search(@RequestParam(required = false) String query) {
        return Mono.fromRunnable(() -> queryValidator.validate(query))
                .thenMany(Flux.defer(() -> externalRecipeService.search(query.trim())))
                .map(recipeMapper::searchItemToResponseDto);
    }

    @Operation(summary = "Загрузить рецепт по id из результатов поиска",
            description = "Для iamcook.ru id — число из /showrecipe/<id>, иначе 400")
    @GetMapping
    public Mono<ExternalRecipeResponseDto> getById(@RequestParam(required = false) String id) {
        return Mono.fromRunnable(() -> {
                    if (!StringUtils.hasText(id)) {
                        throw new BadRequestException("Recipe id is required", "Не указан идентификатор рецепта");
                    }
                })
                .then(Mono.defer(() -> externalRecipeService.getById(id.trim())))
                .map(recipeMapper::recipeToResponseDto);
    }
}
