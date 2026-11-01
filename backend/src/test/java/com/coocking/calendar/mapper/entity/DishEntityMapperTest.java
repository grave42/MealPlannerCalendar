package com.coocking.calendar.mapper.entity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.coocking.calendar.dao.model.entity.DishEntity;
import com.coocking.calendar.dao.model.type.MealType;
import com.coocking.calendar.model.request.DishRequestDto;
import com.coocking.calendar.model.response.DishResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Класс DishEntityMapper")
class DishEntityMapperTest {

    private final DishEntityMapper mapper = Mappers.getMapper(DishEntityMapper.class);

    @Test
    @DisplayName("Запрос преобразуется в сущность без картинки")
    void testRequestToEntity() {
        DishRequestDto dto = new DishRequestDto();
        dto.setName("Омлет");
        dto.setIngredients("Яйца");
        dto.setMealTypes(List.of(MealType.BREAKFAST, MealType.SNACK));
        dto.setSourceImageUrl("https://img.iamcook.ru/a.jpg");

        DishEntity entity = mapper.requestDtoToEntity(dto);

        assertEquals("Омлет", entity.getName());
        assertEquals("Яйца", entity.getIngredients());
        assertEquals(Set.of(MealType.BREAKFAST, MealType.SNACK), entity.getMealTypes());
        assertNull(entity.getImage());
        assertNull(entity.getId());
    }

    @Test
    @DisplayName("Не переданные метки дают пустой набор")
    void testRequestWithoutMealTypes() {
        DishRequestDto dto = new DishRequestDto();
        dto.setName("Омлет");

        assertTrue(mapper.requestDtoToEntity(dto).getMealTypes().isEmpty());
    }

    @Test
    @DisplayName("Ответ содержит флаг hasImage и не содержит байтов картинки")
    void testEntityToResponse() {
        UUID id = UUID.randomUUID();
        DishEntity entity = DishEntity.builder().id(id).name("Омлет").mealTypes(Set.of(MealType.LUNCH))
                .image(new byte[]{1}).imageContentType("image/png").createUser("anonymous").build();

        DishResponseDto response = mapper.entityToResponseDto(entity);

        assertEquals(id, response.getId());
        assertEquals(true, response.getHasImage());
        assertEquals(Set.of(MealType.LUNCH), response.getMealTypes());
        assertEquals("anonymous", response.getCreateUser());
    }

    @Test
    @DisplayName("Блюдо без картинки: hasImage = false")
    void testHasImageFalse() {
        DishEntity entity = DishEntity.builder().name("Омлет").build();

        assertFalse(mapper.entityToResponseDto(entity).getHasImage());
    }

    @Test
    @DisplayName("Патч не затрагивает id, аудит создания и картинку")
    void testPatch() {
        UUID id = UUID.randomUUID();
        DishEntity target = DishEntity.builder().id(id).name("Старое").createUser("alice")
                .image(new byte[]{1, 2}).imageContentType("image/png").mealTypes(Set.of()).build();
        DishEntity source = DishEntity.builder().id(UUID.randomUUID()).name("Новое").createUser("bob")
                .mealTypes(Set.of(MealType.DINNER)).build();

        mapper.patchEntityFromEntity(source, target);

        assertEquals(id, target.getId());
        assertEquals("Новое", target.getName());
        assertEquals("alice", target.getCreateUser());
        assertEquals(Set.of(MealType.DINNER), target.getMealTypes());
        assertArrayEquals(new byte[]{1, 2}, target.getImage());
        assertEquals("image/png", target.getImageContentType());
    }
}
