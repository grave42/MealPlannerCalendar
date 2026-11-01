package com.coocking.calendar.mapper.function;

import com.coocking.calendar.model.response.ExternalRecipeResponseDto;
import com.coocking.calendar.model.response.ExternalRecipeSearchItemResponseDto;
import com.coocking.calendar.service.external.ExternalRecipe;
import com.coocking.calendar.service.external.ExternalRecipeSearchItem;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Маппер моделей внешнего источника рецептов в DTO ответов.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ExternalRecipeMapper {

    ExternalRecipeSearchItemResponseDto searchItemToResponseDto(ExternalRecipeSearchItem item);

    ExternalRecipeResponseDto recipeToResponseDto(ExternalRecipe recipe);
}
