package com.coocking.calendar.service.entity;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.coocking.calendar.dao.model.entity.DishEntity;
import com.coocking.calendar.dao.model.type.MealType;
import com.coocking.calendar.dao.repository.DishR2dbcRepository;
import com.coocking.calendar.exception.BadRequestException;
import com.coocking.calendar.exception.NotFoundException;
import com.coocking.calendar.exception.ValidationException;
import com.coocking.calendar.mapper.entity.DishEntityMapperImpl;
import com.coocking.calendar.service.ImageService;
import com.coocking.calendar.service.external.ExternalImage;
import com.coocking.calendar.service.external.RecipeSourceClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Класс DishService")
@SpringJUnitConfig(classes = {DishService.class, DishEntityMapperImpl.class, ImageService.class})
class DishServiceTest {

    private static final UUID ID = UUID.fromString("0b8f1c3e-5d1a-4c71-9a35-0f7a7a1f2c10");
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};

    @Autowired
    private DishService dishService;

    @MockitoBean
    private DishR2dbcRepository dishRepository;

    @MockitoBean
    private RecipeSourceClient recipeSourceClient;

    private DishEntity createTestDish(String name, Set<MealType> mealTypes) {
        return DishEntity.builder().id(ID).name(name).mealTypes(mealTypes).build();
    }

    private Flux<DataBuffer> buffers(byte[] bytes) {
        return Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes));
    }

    @Nested
    @DisplayName("Создание блюда")
    class Create {

        @Test
        @DisplayName("Блюдо без картинки сохраняется")
        void testOk() {
            DishEntity dish = createTestDish("Омлет", Set.of());
            when(dishRepository.save(dish)).thenReturn(Mono.just(dish));

            StepVerifier.create(dishService.create(dish, null))
                    .expectNext(dish)
                    .verifyComplete();

            verify(recipeSourceClient, never()).downloadImage(anyString());
        }

        @Test
        @DisplayName("Картинка из источника скачивается и сохраняется")
        void testWithSourceImage() {
            DishEntity dish = createTestDish("Паста", Set.of());
            when(recipeSourceClient.downloadImage("https://img.iamcook.ru/a.png"))
                    .thenReturn(Mono.just(ExternalImage.builder().content(PNG).contentType("image/png").build()));
            when(dishRepository.save(any(DishEntity.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

            StepVerifier.create(dishService.create(dish, "https://img.iamcook.ru/a.png"))
                    .assertNext(saved -> {
                        assertArrayEquals(PNG, saved.getImage());
                        assertEquals("image/png", saved.getImageContentType());
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Сбой скачивания картинки — блюдо создаётся без картинки")
        void testSourceImageFailure() {
            DishEntity dish = createTestDish("Паста", Set.of());
            when(recipeSourceClient.getSourceName()).thenReturn("iamcook.ru");
            when(recipeSourceClient.downloadImage(anyString()))
                    .thenReturn(Mono.error(new BadRequestException("Image url rejected", "Адрес картинки не принадлежит")));
            when(dishRepository.save(any(DishEntity.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

            StepVerifier.create(dishService.create(dish, "https://evil.example/a.png"))
                    .assertNext(saved -> {
                        assertNull(saved.getImage());
                        assertNull(saved.getImageContentType());
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Скачанный файл неверного типа — блюдо создаётся без картинки")
        void testSourceImageInvalidContent() {
            DishEntity dish = createTestDish("Паста", Set.of());
            when(recipeSourceClient.getSourceName()).thenReturn("iamcook.ru");
            when(recipeSourceClient.downloadImage(anyString())).thenReturn(Mono.just(ExternalImage.builder()
                    .content("<html>".getBytes(StandardCharsets.UTF_8)).contentType("image/jpeg").build()));
            when(dishRepository.save(any(DishEntity.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

            StepVerifier.create(dishService.create(dish, "https://img.iamcook.ru/a.jpg"))
                    .assertNext(saved -> assertNull(saved.getImage()))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Обновление блюда")
    class Update {

        @Test
        @DisplayName("Поля обновляются, картинка сохраняется")
        void testOk() {
            DishEntity existing = createTestDish("Старое", Set.of());
            existing.setImage(PNG);
            existing.setImageContentType("image/png");
            existing.setCreateUser("anonymous");
            DishEntity patch = DishEntity.builder().name("Новое").description("Описание")
                    .mealTypes(new LinkedHashSet<>(Set.of(MealType.SNACK))).build();
            when(dishRepository.findById(ID)).thenReturn(Mono.just(existing));
            when(dishRepository.save(any(DishEntity.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

            StepVerifier.create(dishService.update(ID, patch))
                    .assertNext(saved -> {
                        assertEquals(ID, saved.getId());
                        assertEquals("Новое", saved.getName());
                        assertEquals(Set.of(MealType.SNACK), saved.getMealTypes());
                        assertArrayEquals(PNG, saved.getImage());
                        assertEquals("anonymous", saved.getCreateUser());
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Несуществующее блюдо — NotFoundException")
        void testNotFound() {
            when(dishRepository.findById(ID)).thenReturn(Mono.empty());

            StepVerifier.create(dishService.update(ID, createTestDish("Новое", Set.of())))
                    .expectError(NotFoundException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("Удаление блюда")
    class Delete {

        @Test
        @DisplayName("Существующее блюдо удаляется")
        void testOk() {
            when(dishRepository.existsById(ID)).thenReturn(Mono.just(true));
            when(dishRepository.deleteById(ID)).thenReturn(Mono.empty());

            StepVerifier.create(dishService.deleteById(ID)).verifyComplete();

            verify(dishRepository).deleteById(ID);
        }

        @Test
        @DisplayName("Несуществующее блюдо — NotFoundException")
        void testNotFound() {
            when(dishRepository.existsById(ID)).thenReturn(Mono.just(false));

            StepVerifier.create(dishService.deleteById(ID)).expectError(NotFoundException.class).verify();

            verify(dishRepository, never()).deleteById(ID);
        }
    }

    @Nested
    @DisplayName("Выборка блюд по правилу меток")
    class FindAll {

        @Test
        @DisplayName("С mealType репозиторию передаётся имя метки и шаблон поиска")
        void testWithMealType() {
            DishEntity withLabel = createTestDish("Омлет", Set.of(MealType.BREAKFAST));
            DishEntity withoutLabels = createTestDish("Салат", Set.of());
            when(dishRepository.findAllSummaries("BREAKFAST", "%")).thenReturn(Flux.just(withLabel, withoutLabels));

            StepVerifier.create(dishService.findAll(MealType.BREAKFAST, null))
                    .expectNext(withLabel, withoutLabels)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Без mealType передаётся пустой фильтр, поиск приводится к нижнему регистру и экранируется")
        void testWithoutMealTypeAndSearch() {
            when(dishRepository.findAllSummaries("", "%100\\%\\_суп%")).thenReturn(Flux.empty());

            StepVerifier.create(dishService.findAll(null, "  100%_СУП ")).verifyComplete();

            verify(dishRepository).findAllSummaries("", "%100\\%\\_суп%");
        }
    }

    @Nested
    @DisplayName("Картинка блюда")
    class ImageOperations {

        @Test
        @DisplayName("Загрузка PNG сохраняет картинку и определённый тип")
        void testSetImageOk() {
            when(dishRepository.findById(ID)).thenReturn(Mono.just(createTestDish("Омлет", Set.of())));
            when(dishRepository.save(any(DishEntity.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

            StepVerifier.create(dishService.setImage(ID, buffers(PNG)))
                    .assertNext(saved -> {
                        assertEquals("image/png", saved.getImageContentType());
                        assertArrayEquals(PNG, saved.getImage());
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Файл неверного типа — ValidationException, сохранения нет")
        void testSetImageInvalidType() {
            when(dishRepository.findById(ID)).thenReturn(Mono.just(createTestDish("Омлет", Set.of())));

            StepVerifier.create(dishService.setImage(ID, buffers("text".getBytes(StandardCharsets.UTF_8))))
                    .expectError(ValidationException.class)
                    .verify();

            verify(dishRepository, never()).save(any(DishEntity.class));
        }

        @Test
        @DisplayName("Файл больше 5 МБ — ValidationException")
        void testSetImageTooLarge() {
            byte[] big = new byte[ImageService.MAX_IMAGE_SIZE + 10];

            StepVerifier.create(dishService.setImage(ID, buffers(big)))
                    .expectError(ValidationException.class)
                    .verify();
        }

        @Test
        @DisplayName("Удаление картинки обнуляет поля")
        void testRemoveImage() {
            DishEntity dish = createTestDish("Омлет", Set.of());
            dish.setImage(PNG);
            dish.setImageContentType("image/png");
            when(dishRepository.findById(ID)).thenReturn(Mono.just(dish));
            when(dishRepository.save(any(DishEntity.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

            StepVerifier.create(dishService.removeImage(ID))
                    .assertNext(saved -> {
                        assertNull(saved.getImage());
                        assertNull(saved.getImageContentType());
                    })
                    .verifyComplete();
            ArgumentCaptor<DishEntity> captor = ArgumentCaptor.forClass(DishEntity.class);
            verify(dishRepository).save(captor.capture());
            assertNull(captor.getValue().getImage());
        }

        @Test
        @DisplayName("Блюдо без картинки — NotFoundException при чтении картинки")
        void testFindImageMissing() {
            when(dishRepository.findImageById(ID)).thenReturn(Mono.just(createTestDish("Омлет", Set.of())));

            StepVerifier.create(dishService.findImage(ID)).expectError(NotFoundException.class).verify();
        }

        @Test
        @DisplayName("Блюдо с картинкой возвращается при чтении картинки")
        void testFindImageOk() {
            DishEntity dish = createTestDish("Омлет", Set.of());
            dish.setImage(PNG);
            dish.setImageContentType("image/png");
            when(dishRepository.findImageById(ID)).thenReturn(Mono.just(dish));

            StepVerifier.create(dishService.findImage(ID)).expectNext(dish).verifyComplete();
        }
    }
}
