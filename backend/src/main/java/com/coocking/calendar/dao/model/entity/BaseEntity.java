package com.coocking.calendar.dao.model.entity;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;

/**
 * Базовая сущность с идентификатором и аудит-полями.
 * Аудит-поля заполняются {@code AuditingEntityCallback} из Reactor Context.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEntity {

    /** Поле БД - id */
    @Id
    @Column("id")
    private UUID id;

    /** Поле БД - create_time */
    @Column("create_time")
    private Instant createTime;

    /** Поле БД - create_user */
    @Column("create_user")
    private String createUser;

    /** Поле БД - last_modify_time */
    @Column("last_modify_time")
    private Instant lastModifyTime;

    /** Поле БД - last_modify_user */
    @Column("last_modify_user")
    private String lastModifyUser;
}
