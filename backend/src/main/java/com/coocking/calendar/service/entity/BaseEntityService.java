package com.coocking.calendar.service.entity;

import com.coocking.calendar.dao.model.entity.BaseEntity;
import com.coocking.calendar.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Базовый CRUD-сервис сущностей.
 *
 * @param <E>  тип сущности
 * @param <ID> тип идентификатора
 */
@Slf4j
public abstract class BaseEntityService<E extends BaseEntity, ID> {

    private final R2dbcRepository<E, ID> repository;
    private final String entityName;
    private final String notFoundUserMessage;

    /**
     * @param repository          репозиторий сущности
     * @param entityName          имя сущности для сообщений разработчика (английский)
     * @param notFoundUserMessage сообщение пользователю, если сущность не найдена (русский)
     */
    protected BaseEntityService(R2dbcRepository<E, ID> repository, String entityName, String notFoundUserMessage) {
        this.repository = repository;
        this.entityName = entityName;
        this.notFoundUserMessage = notFoundUserMessage;
    }

    /**
     * Создаёт сущность.
     */
    @Transactional
    public Mono<E> create(E entity) {
        return Mono.deferContextual(Mono::just)
                .flatMap(ctx -> repository.save(entity))
                .doOnSuccess(saved -> log.info("Создана сущность {}, id={}", entityName, saved.getId()))
                .doOnError(e -> log.error("Не удалось создать сущность {}", entityName, e));
    }

    /**
     * Сохраняет изменения существующей сущности.
     */
    @Transactional
    public Mono<E> update(E entity) {
        return Mono.deferContextual(Mono::just)
                .flatMap(ctx -> repository.save(entity))
                .doOnSuccess(saved -> log.info("Обновлена сущность {}, id={}", entityName, saved.getId()))
                .doOnError(e -> log.error("Не удалось обновить сущность {}, id={}", entityName, entity.getId(), e));
    }

    /**
     * Ищет сущность по идентификатору.
     *
     * @throws NotFoundException если сущность не найдена
     */
    public Mono<E> findById(ID id) {
        return Mono.deferContextual(Mono::just)
                .flatMap(ctx -> repository.findById(id))
                .switchIfEmpty(Mono.error(() -> notFound(id)));
    }

    /**
     * Возвращает все сущности.
     */
    public Flux<E> findAll() {
        return repository.findAll();
    }

    /**
     * Удаляет сущность по идентификатору.
     */
    @Transactional
    public Mono<Void> deleteById(ID id) {
        return Mono.deferContextual(Mono::just)
                .flatMap(ctx -> repository.existsById(id))
                .flatMap(exists -> Boolean.TRUE.equals(exists)
                        ? repository.deleteById(id)
                        : Mono.<Void>error(notFound(id)))
                .doOnSuccess(v -> log.info("Удалена сущность {}, id={}", entityName, id))
                .doOnError(e -> log.warn("Не удалось удалить сущность {}, id={}: {}", entityName, id, e.getMessage()));
    }

    protected NotFoundException notFound(ID id) {
        return new NotFoundException(entityName + " not found: " + id, notFoundUserMessage);
    }
}
