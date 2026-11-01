package com.coocking.calendar.config;

import com.coocking.calendar.config.properties.ExternalRecipeProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Настройка {@link WebClient} для обращения к внешнему источнику рецептов.
 */
@Configuration
public class ExternalRecipeConfig {

    private static final int MAX_IN_MEMORY_SIZE = 4 * 1024 * 1024;

    @Bean
    public WebClient externalRecipeWebClient(WebClient.Builder builder, ExternalRecipeProperties properties) {
        return builder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, properties.getUserAgent())
                .defaultHeader(HttpHeaders.REFERER, properties.getBaseUrl() + "/")
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE))
                .build();
    }
}
