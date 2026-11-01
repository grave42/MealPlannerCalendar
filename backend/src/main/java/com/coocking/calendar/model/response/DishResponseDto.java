package com.coocking.calendar.model.response;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.coocking.calendar.dao.model.type.MealType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Ответ с данными блюда (без байтов картинки).
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Блюдо")
public class DishResponseDto implements ResponseEntity {

    @Schema(description = "Идентификатор блюда", example = "0b8f1c3e-5d1a-4c71-9a35-0f7a7a1f2c10")
    private UUID id;

    @Schema(description = "Название", example = "Омлет")
    private String name;

    @Schema(description = "Описание", example = "Нежный омлет на молоке")
    private String description;

    @Schema(description = "Состав для приготовления", example = "Яйца - 3 шт.")
    private String ingredients;

    @Schema(description = "Описание приготовления", example = "Взбейте яйца и обжарьте")
    private String instructions;

    @Schema(description = "Метки приёмов пищи", example = "[\"BREAKFAST\"]")
    private Set<MealType> mealTypes;

    @Schema(description = "Есть ли у блюда картинка (GET /api/v1/dish/{id}/image)", example = "true")
    private Boolean hasImage;

    @Schema(description = "Время создания", accessMode = Schema.AccessMode.READ_ONLY)
    private Instant createTime;

    @Schema(description = "Кто создал (идентификатор клиента)", example = "anonymous",
            accessMode = Schema.AccessMode.READ_ONLY)
    private String createUser;

    @Schema(description = "Время последнего изменения", accessMode = Schema.AccessMode.READ_ONLY)
    private Instant lastModifyTime;

    @Schema(description = "Кто изменил последним (идентификатор клиента)", example = "anonymous",
            accessMode = Schema.AccessMode.READ_ONLY)
    private String lastModifyUser;
}
