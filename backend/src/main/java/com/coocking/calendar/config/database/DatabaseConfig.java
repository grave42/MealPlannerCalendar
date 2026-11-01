package com.coocking.calendar.config.database;

import com.coocking.calendar.dao.converter.InstantToLongConverter;
import com.coocking.calendar.dao.converter.JsonToMealTypeSetConverter;
import com.coocking.calendar.dao.converter.LongToInstantConverter;
import com.coocking.calendar.dao.converter.MealTypeSetToJsonConverter;
import com.coocking.calendar.dao.converter.MealTypeToStringConverter;
import com.coocking.calendar.dao.converter.StringToMealTypeConverter;
import com.coocking.calendar.dao.model.entity.BaseEntity;
import com.coocking.calendar.utils.ContextUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * Конфигурация R2DBC: конвертеры и заполнение аудит-полей.
 */
@Configuration
public class DatabaseConfig {

    /**
     * Регистрирует пользовательские конвертеры типов.
     */
    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        return R2dbcCustomConversions.of(PostgresDialect.INSTANCE, List.of(
                new MealTypeToStringConverter(),
                new StringToMealTypeConverter(),
                new MealTypeSetToJsonConverter(),
                new JsonToMealTypeSetConverter(),
                new InstantToLongConverter(),
                new LongToInstantConverter()));
    }

    /**
     * Заполняет аудит-поля сущностей значениями из Reactor Context.
     */
    @Bean
    public BeforeConvertCallback<BaseEntity> auditingEntityCallback() {
        return (entity, table) -> Mono.deferContextual(ctx -> {
            String user = ctx.getOrDefault(ContextUtil.CLIENT_ID, ContextUtil.ANONYMOUS_CLIENT);
            Instant now = Instant.now();
            if (entity.getCreateTime() == null) {
                entity.setCreateTime(now);
                entity.setCreateUser(user);
            }
            entity.setLastModifyTime(now);
            entity.setLastModifyUser(user);
            return Mono.just(entity);
        });
    }
}
