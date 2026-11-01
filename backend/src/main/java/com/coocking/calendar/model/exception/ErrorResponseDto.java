package com.coocking.calendar.model.exception;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Ответ с описанием ошибки.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Описание ошибки")
public class ErrorResponseDto {

    @Schema(description = "HTTP-статус", example = "400")
    private Integer status;

    @Schema(description = "Техническое сообщение (для разработчика)", example = "Dish name must not be blank")
    private String message;

    @Schema(description = "Сообщение для пользователя", example = "Введите название блюда")
    private String userMessage;

    @Schema(description = "Идентификатор запроса", example = "0b8f1c3e-5d1a-4c71-9a35-0f7a7a1f2c10")
    private String requestId;

    @Schema(description = "Время ошибки")
    private Instant timestamp;
}
