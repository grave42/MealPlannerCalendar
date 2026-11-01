package com.coocking.calendar.controller;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.coocking.calendar.dao.model.entity.DishEntity;
import com.coocking.calendar.dao.model.type.MealType;
import com.coocking.calendar.exception.NotFoundException;
import com.coocking.calendar.exception.ValidationException;
import com.coocking.calendar.exception.handler.GlobalExceptionHandler;
import com.coocking.calendar.mapper.entity.DishEntityMapperImpl;
import com.coocking.calendar.service.entity.DishService;
import com.coocking.calendar.utils.validator.DishPayloadValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Класс DishController")
@WebFluxTest(DishController.class)
@ContextConfiguration(classes = {DishController.class, DishEntityMapperImpl.class, DishPayloadValidator.class,
        GlobalExceptionHandler.class})
class DishControllerTest {

    private static final UUID ID = UUID.fromString("0b8f1c3e-5d1a-4c71-9a35-0f7a7a1f2c10");

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private DishService dishService;

    private DishEntity createTestDish(String name, boolean withImage) {
        return DishEntity.builder()
                .id(ID)
                .name(name)
                .description("Описание")
                .mealTypes(Set.of(MealType.BREAKFAST))
                .imageContentType(withImage ? "image/png" : null)
                .image(withImage ? new byte[]{1, 2, 3} : null)
                .createTime(Instant.parse("2026-11-01T00:00:00Z"))
                .lastModifyTime(Instant.parse("2026-11-01T00:00:00Z"))
                .createUser("anonymous")
                .lastModifyUser("anonymous")
                .build();
    }

    @Nested
    @DisplayName("Создание блюда")
    class Create {

        @Test
        @DisplayName("Блюдо только с названием создаётся")
        void testOk() {
            when(dishService.create(any(DishEntity.class), eq(null))).thenReturn(Mono.just(createTestDish("Омлет", false)));

            webTestClient.post().uri("/api/v1/dish")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"name\":\"Омлет\"}")
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.name").isEqualTo("Омлет")
                    .jsonPath("$.hasImage").isEqualTo(false);
        }

        @Test
        @DisplayName("Пустое название даёт 400 с userMessage")
        void testEmptyName() {
            webTestClient.post().uri("/api/v1/dish")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"name\":\"  \"}")
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.userMessage").isEqualTo("Введите название блюда");

            verify(dishService, never()).create(any(DishEntity.class), any());
        }

        @Test
        @DisplayName("Название длиннее 200 символов даёт 400")
        void testLongName() {
            webTestClient.post().uri("/api/v1/dish")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"name\":\"" + "а".repeat(201) + "\"}")
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.userMessage").isEqualTo("Название не длиннее 200 символов");
        }

        @Test
        @DisplayName("Дубликаты меток дают 400")
        void testDuplicateMealTypes() {
            webTestClient.post().uri("/api/v1/dish")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"name\":\"Омлет\",\"mealTypes\":[\"LUNCH\",\"LUNCH\"]}")
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("Неизвестная метка даёт 400")
        void testUnknownMealType() {
            webTestClient.post().uri("/api/v1/dish")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"name\":\"Омлет\",\"mealTypes\":[\"BRUNCH\"]}")
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("sourceImageUrl передаётся в сервис")
        void testSourceImageUrl() {
            when(dishService.create(any(DishEntity.class), eq("https://img.iamcook.ru/a.jpg")))
                    .thenReturn(Mono.just(createTestDish("Паста", true)));

            webTestClient.post().uri("/api/v1/dish")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"name\":\"Паста\",\"sourceImageUrl\":\"https://img.iamcook.ru/a.jpg\"}")
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody().jsonPath("$.hasImage").isEqualTo(true);
        }
    }

    @Nested
    @DisplayName("Список блюд")
    class FindAll {

        @Test
        @DisplayName("Фильтры mealType и search передаются в сервис")
        void testOk() {
            when(dishService.findAll(MealType.LUNCH, "суп"))
                    .thenReturn(Flux.just(createTestDish("Суп", false)));

            webTestClient.get().uri("/api/v1/dish?mealType=LUNCH&search=суп")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.length()").isEqualTo(1)
                    .jsonPath("$[0].name").isEqualTo("Суп");
        }

        @Test
        @DisplayName("Без параметров возвращаются все блюда")
        void testNoFilters() {
            when(dishService.findAll(null, null)).thenReturn(Flux.empty());

            webTestClient.get().uri("/api/v1/dish").exchange()
                    .expectStatus().isOk()
                    .expectBody().json("[]");
        }

        @Test
        @DisplayName("Неизвестный mealType даёт 400")
        void testInvalidMealType() {
            webTestClient.get().uri("/api/v1/dish?mealType=BRUNCH").exchange()
                    .expectStatus().isBadRequest()
                    .expectBody().jsonPath("$.userMessage").isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Получение блюда")
    class FindById {

        @Test
        @DisplayName("Существующее блюдо возвращается")
        void testOk() {
            when(dishService.findSummaryById(ID)).thenReturn(Mono.just(createTestDish("Омлет", true)));

            webTestClient.get().uri("/api/v1/dish/" + ID).exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.id").isEqualTo(ID.toString())
                    .jsonPath("$.hasImage").isEqualTo(true)
                    .jsonPath("$.image").doesNotExist();
        }

        @Test
        @DisplayName("Несуществующее блюдо даёт 404")
        void testNotFound() {
            when(dishService.findSummaryById(ID))
                    .thenReturn(Mono.error(new NotFoundException("Dish not found", "Блюдо не найдено")));

            webTestClient.get().uri("/api/v1/dish/" + ID).exchange()
                    .expectStatus().isNotFound()
                    .expectBody().jsonPath("$.userMessage").isEqualTo("Блюдо не найдено");
        }
    }

    @Nested
    @DisplayName("Обновление и удаление")
    class UpdateAndDelete {

        @Test
        @DisplayName("Обновление блюда")
        void testUpdateOk() {
            when(dishService.update(eq(ID), any(DishEntity.class))).thenReturn(Mono.just(createTestDish("Новое", false)));

            webTestClient.put().uri("/api/v1/dish/" + ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"name\":\"Новое\",\"mealTypes\":[\"BREAKFAST\"]}")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody().jsonPath("$.name").isEqualTo("Новое");
        }

        @Test
        @DisplayName("Обновление с пустым названием даёт 400")
        void testUpdateInvalid() {
            webTestClient.put().uri("/api/v1/dish/" + ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"name\":\"\"}")
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("Удаление блюда")
        void testDeleteOk() {
            when(dishService.deleteById(ID)).thenReturn(Mono.empty());

            webTestClient.delete().uri("/api/v1/dish/" + ID).exchange()
                    .expectStatus().isNoContent();
        }

        @Test
        @DisplayName("Удаление несуществующего блюда даёт 404")
        void testDeleteNotFound() {
            when(dishService.deleteById(ID))
                    .thenReturn(Mono.error(new NotFoundException("Dish not found", "Блюдо не найдено")));

            webTestClient.delete().uri("/api/v1/dish/" + ID).exchange()
                    .expectStatus().isNotFound();
        }
    }

    @Nested
    @DisplayName("Картинка блюда")
    class Image {

        private WebTestClient.ResponseSpec upload() {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", new byte[]{1, 2, 3}).filename("a.png").contentType(MediaType.IMAGE_PNG);
            return webTestClient.put().uri("/api/v1/dish/" + ID + "/image")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .exchange();
        }

        @Test
        @DisplayName("Загрузка картинки")
        void testUploadOk() {
            when(dishService.setImage(eq(ID), any())).thenReturn(Mono.just(createTestDish("Омлет", true)));

            upload().expectStatus().isOk()
                    .expectBody().jsonPath("$.hasImage").isEqualTo(true);
        }

        @Test
        @DisplayName("Слишком большая или неверного типа картинка даёт 400 с userMessage")
        void testUploadInvalid() {
            when(dishService.setImage(eq(ID), any())).thenReturn(Mono.error(new ValidationException(
                    "Unsupported image type", "Допустимы изображения JPG, PNG, WEBP до 5 МБ")));

            upload().expectStatus().isBadRequest()
                    .expectBody().jsonPath("$.userMessage").isEqualTo("Допустимы изображения JPG, PNG, WEBP до 5 МБ");
        }

        @Test
        @DisplayName("Запрос без части file даёт 400")
        void testUploadWithoutFile() {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("other", "x");
            webTestClient.put().uri("/api/v1/dish/" + ID + "/image")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("Удаление картинки")
        void testDeleteOk() {
            when(dishService.removeImage(ID)).thenReturn(Mono.just(createTestDish("Омлет", false)));

            webTestClient.delete().uri("/api/v1/dish/" + ID + "/image").exchange()
                    .expectStatus().isOk()
                    .expectBody().jsonPath("$.hasImage").isEqualTo(false);
        }

        @Test
        @DisplayName("Отдача картинки с Content-Type и Cache-Control")
        void testGetOk() {
            when(dishService.findImage(ID)).thenReturn(Mono.just(createTestDish("Омлет", true)));

            webTestClient.get().uri("/api/v1/dish/" + ID + "/image").exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(MediaType.IMAGE_PNG)
                    .expectHeader().valueEquals("Cache-Control", "no-cache")
                    .expectBody(byte[].class).isEqualTo(new byte[]{1, 2, 3});
        }

        @Test
        @DisplayName("Нет картинки — 404")
        void testGetNotFound() {
            when(dishService.findImage(ID))
                    .thenReturn(Mono.error(new NotFoundException("Dish image not found", "У блюда нет картинки")));

            webTestClient.get().uri("/api/v1/dish/" + ID + "/image").exchange()
                    .expectStatus().isNotFound();
        }
    }
}
