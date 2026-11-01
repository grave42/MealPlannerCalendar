package com.coocking.calendar.mapper.entity;

import com.coocking.calendar.dao.model.entity.BaseEntity;
import org.mapstruct.MappingTarget;

/**
 * Базовый интерфейс мапперов сущностей.
 *
 * @param <E> тип сущности
 */
public interface BaseEntityMapper<E extends BaseEntity> {

    /**
     * Переносит изменяемые поля из {@code source} в {@code target}
     * (без {@code id}, {@code createTime}, {@code createUser}).
     */
    void patchEntityFromEntity(E source, @MappingTarget E target);
}
