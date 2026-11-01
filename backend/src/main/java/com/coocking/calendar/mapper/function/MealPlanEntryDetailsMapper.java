package com.coocking.calendar.mapper.function;

import com.coocking.calendar.dao.model.projection.MealPlanEntryDetails;
import com.coocking.calendar.model.response.MealPlanEntryResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Маппер записи плана вместе с краткими данными блюда.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MealPlanEntryDetailsMapper {

    @Mapping(target = "date", source = "planDate")
    MealPlanEntryResponseDto detailsToResponseDto(MealPlanEntryDetails details);
}
