package com.coocking.calendar.mapper.entity;

import com.coocking.calendar.dao.model.entity.DishEntity;
import com.coocking.calendar.model.request.DishRequestDto;
import com.coocking.calendar.model.response.DishResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * Маппер блюд: сущность и DTO.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DishEntityMapper extends BaseEntityMapper<DishEntity> {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "image", ignore = true)
    @Mapping(target = "imageContentType", ignore = true)
    DishEntity requestDtoToEntity(DishRequestDto dto);

    @Mapping(target = "hasImage", expression = "java(entity.hasImage())")
    DishResponseDto entityToResponseDto(DishEntity entity);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "createUser", ignore = true)
    @Mapping(target = "image", ignore = true)
    @Mapping(target = "imageContentType", ignore = true)
    void patchEntityFromEntity(DishEntity source, @MappingTarget DishEntity target);
}
