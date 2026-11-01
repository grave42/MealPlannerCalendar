package com.coocking.calendar.controller;

import com.coocking.calendar.exception.BadRequestException;
import com.coocking.calendar.exception.ExternalServiceException;
import com.coocking.calendar.exception.handler.GlobalExceptionHandler;
import com.coocking.calendar.mapper.function.ExternalRecipeMapperImpl;
import com.coocking.calendar.service.ExternalRecipeService;
import com.coocking.calendar.service.external.ExternalRecipe;
import com.coocking.calendar.service.external.ExternalRecipeSearchItem;
import com.coocking.calendar.utils.validator.ExternalRecipeQueryValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Класс ExternalRecipeController")
@WebFluxTest(ExternalRecipeController.class)
@ContextConfiguration(classes = {ExternalRecipeController.class, ExternalRecipeMapperImpl.class,
        ExternalRecipeQueryValidator.class, GlobalExceptionHandler.class})
class ExternalRecipeControllerTest {

    private static final String SOURCE_ERROR = "Не удалось получить данные с iamcook.ru. "
            + "Попробуйте позже или заполните блюдо вручную";

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ExternalRecipeService externalRecipeService;

    @Nested
    @DisplayName("Поиск рецептов")
    class Search {

        @Test
        @DisplayName("Возвращается список найденных рецептов")
        void testOk() {
            when(externalRecipeService.search("паста")).thenReturn(Flux.just(ExternalRecipeSearchItem.builder()
                    .id("37207").title("Паста аль-помодоро").imageUrl("https://img.iamcook.ru/a.jpg").build()));

            webTestClient.get().uri("/api/v1/external-recipe/search?query=паста").exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.length()").isEqualTo(1)
                    .jsonPath("$[0].id").isEqualTo("37207")
                    .jsonPath("$[0].title").isEqualTo("Паста аль-помодоро")
                    .jsonPath("$[0].imageUrl").isEqualTo("https://img.iamcook.ru/a.jpg");
        }

        @Test
        @DisplayName("Пустой результат — пустой список")
        void testEmpty() {
            when(externalRecipeService.search("ничего")).thenReturn(Flux.empty());

            webTestClient.get().uri("/api/v1/external-recipe/search?query=ничего").exchange()
                    .expectStatus().isOk()
                    .expectBody().json("[]");
        }

        @Test
        @DisplayName("Запрос короче 2 символов или отсутствующий даёт 400")
        void testShortQuery() {
            webTestClient.get().uri("/api/v1/external-recipe/search?query=а").exchange()
                    .expectStatus().isBadRequest()
                    .expectBody().jsonPath("$.userMessage").isNotEmpty();
            webTestClient.get().uri("/api/v1/external-recipe/search").exchange()
                    .expectStatus().isBadRequest();

            verify(externalRecipeService, never()).search(any());
        }

        @Test
        @DisplayName("Недоступность источника даёт 502 с userMessage")
        void testSourceFailure() {
            when(externalRecipeService.search("паста")).thenReturn(Flux.error(
                    new ExternalServiceException("iamcook.ru request failed", SOURCE_ERROR)));

            webTestClient.get().uri("/api/v1/external-recipe/search?query=паста").exchange()
                    .expectStatus().isEqualTo(502)
                    .expectBody().jsonPath("$.userMessage").isEqualTo(SOURCE_ERROR);
        }
    }

    @Nested
    @DisplayName("Загрузка рецепта")
    class GetById {

        @Test
        @DisplayName("Возвращается рецепт")
        void testOk() {
            when(externalRecipeService.getById("37207")).thenReturn(Mono.just(ExternalRecipe.builder()
                    .id("37207").title("Паста").description("Описание").ingredients("Паста - 450 г")
                    .instructions("Сварить").imageUrl("https://img.iamcook.ru/a.jpg").build()));

            webTestClient.get().uri("/api/v1/external-recipe?id=37207").exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.title").isEqualTo("Паста")
                    .jsonPath("$.ingredients").isEqualTo("Паста - 450 г")
                    .jsonPath("$.imageUrl").isEqualTo("https://img.iamcook.ru/a.jpg");
        }

        @Test
        @DisplayName("Некорректный id отклоняется источником с 400")
        void testBadId() {
            when(externalRecipeService.getById("../../etc/passwd")).thenReturn(Mono.error(
                    new BadRequestException("Invalid recipe id", "Некорректный идентификатор рецепта")));

            webTestClient.get().uri("/api/v1/external-recipe?id=../../etc/passwd").exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("Отсутствующий id даёт 400")
        void testMissingId() {
            webTestClient.get().uri("/api/v1/external-recipe").exchange()
                    .expectStatus().isBadRequest();

            verify(externalRecipeService, never()).getById(any());
        }

        @Test
        @DisplayName("Недоступность источника даёт 502")
        void testSourceFailure() {
            when(externalRecipeService.getById("1")).thenReturn(Mono.error(
                    new ExternalServiceException("failed", SOURCE_ERROR)));

            webTestClient.get().uri("/api/v1/external-recipe?id=1").exchange()
                    .expectStatus().isEqualTo(502);
        }
    }
}
