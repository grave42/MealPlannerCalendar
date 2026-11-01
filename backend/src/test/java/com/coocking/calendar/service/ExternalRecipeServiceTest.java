package com.coocking.calendar.service;

import java.util.stream.IntStream;

import com.coocking.calendar.config.properties.ExternalRecipeProperties;
import com.coocking.calendar.exception.ExternalServiceException;
import com.coocking.calendar.service.external.ExternalRecipe;
import com.coocking.calendar.service.external.ExternalRecipeSearchItem;
import com.coocking.calendar.service.external.RecipeSourceClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@DisplayName("Класс ExternalRecipeService")
@SpringJUnitConfig(ExternalRecipeServiceTest.TestConfig.class)
class ExternalRecipeServiceTest {

    @Configuration
    @Import(ExternalRecipeService.class)
    static class TestConfig {

        @Bean
        ExternalRecipeProperties externalRecipeProperties() {
            return new ExternalRecipeProperties();
        }
    }

    @Autowired
    private ExternalRecipeService service;

    @MockitoBean
    private RecipeSourceClient recipeSourceClient;

    private ExternalRecipeSearchItem createItem(int n) {
        return ExternalRecipeSearchItem.builder().id(String.valueOf(n)).title("Рецепт " + n).build();
    }

    @Test
    @DisplayName("Поиск проксируется в источник с лимитом из настроек")
    void testSearchOk() {
        when(recipeSourceClient.getSourceName()).thenReturn("test-source");
        when(recipeSourceClient.search("паста", 10)).thenReturn(Flux.just(createItem(1), createItem(2)));

        StepVerifier.create(service.search("паста"))
                .expectNext(createItem(1), createItem(2))
                .verifyComplete();
    }

    @Test
    @DisplayName("Результатов не больше 10, даже если источник вернул больше")
    void testSearchLimit() {
        when(recipeSourceClient.getSourceName()).thenReturn("test-source");
        when(recipeSourceClient.search("паста", 10))
                .thenReturn(Flux.fromStream(IntStream.rangeClosed(1, 15).mapToObj(this::createItem)));

        StepVerifier.create(service.search("паста")).expectNextCount(10).verifyComplete();
    }

    @Test
    @DisplayName("Ошибка источника пробрасывается без изменений")
    void testSearchError() {
        ExternalServiceException error = new ExternalServiceException("failed", "Источник недоступен");
        when(recipeSourceClient.getSourceName()).thenReturn("test-source");
        when(recipeSourceClient.search("паста", 10)).thenReturn(Flux.error(error));

        StepVerifier.create(service.search("паста")).expectErrorMatches(e -> e == error).verify();
    }

    @Test
    @DisplayName("Рецепт загружается по id через интерфейс источника")
    void testGetByIdOk() {
        ExternalRecipe recipe = ExternalRecipe.builder().id("5").title("Рецепт").build();
        when(recipeSourceClient.getSourceName()).thenReturn("test-source");
        when(recipeSourceClient.getById("5")).thenReturn(Mono.just(recipe));

        StepVerifier.create(service.getById("5")).expectNext(recipe).verifyComplete();
    }

    @Test
    @DisplayName("Ошибка загрузки рецепта пробрасывается без изменений")
    void testGetByIdError() {
        ExternalServiceException error = new ExternalServiceException("failed", "Источник недоступен");
        when(recipeSourceClient.getSourceName()).thenReturn("test-source");
        when(recipeSourceClient.getById("5")).thenReturn(Mono.error(error));

        StepVerifier.create(service.getById("5")).expectErrorMatches(e -> e == error).verify();
    }
}
