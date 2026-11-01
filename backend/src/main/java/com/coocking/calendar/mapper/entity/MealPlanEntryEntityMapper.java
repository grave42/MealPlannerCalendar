package com.coocking.calendar.mapper.entity;

import com.coocking.calendar.dao.model.entity.MealPlanEntryEntity;
import com.coocking.calendar.model.request.MealPlanEntryRequestDto;
import com.coocking.calendar.model.response.MealPlanEntryResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * Маппер записей плана питания: сущность и DTO.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MealPlanEntryEntityMapper extends BaseEntityMapper<MealPlanEntryEntity> {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "planDate", source = "date")
    @Mapping(target = "position", ignore = true)
    MealPlanEntryEntity requestDtoToEntity(MealPlanEntryRequestDto dto);

    @Mapping(target = "date", source = "planDate")
    @Mapping(target = "dishName", ignore = true)
    @Mapping(target = "hasImage", ignore = true)
    MealPlanEntryResponseDto entityToResponseDto(MealPlanEntryEntity entity);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "createUser", ignore = true)
    void patchEntityFromEntity(MealPlanEntryEntity source, @MappingTarget MealPlanEntryEntity target);
}
