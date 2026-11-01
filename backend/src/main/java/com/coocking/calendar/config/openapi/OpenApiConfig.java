package com.coocking.calendar.config.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Описание API для springdoc.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI calendarOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Планировщик питания: API")
                .description("Календарь питания, картотека блюд и импорт рецептов с внешнего ресурса")
                .version("1.0"));
    }
}
