package com.coocking.calendar.exception.handler;

import java.time.Instant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import com.coocking.calendar.exception.BadRequestException;
import com.coocking.calendar.exception.CalendarException;
import com.coocking.calendar.exception.ExternalServiceException;
import com.coocking.calendar.exception.ImpossibleOperationException;
import com.coocking.calendar.exception.NotFoundException;
import com.coocking.calendar.exception.ValidationException;
import com.coocking.calendar.model.exception.ErrorResponseDto;
import com.coocking.calendar.utils.ContextUtil;

/**
 * Единственное место преобразования исключений в HTTP-ответы.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String UNKNOWN_ERROR_MESSAGE = "Внутренняя ошибка сервера. Попробуйте позже";
    private static final String BAD_REQUEST_MESSAGE = "Некорректный запрос";
    private static final String TOO_LARGE_MESSAGE = "Размер запроса превышает допустимый (6 МБ)";

    @ExceptionHandler(CalendarException.class)
    public ResponseEntity<ErrorResponseDto> handleCalendarException(CalendarException e, ServerWebExchange exchange) {
        HttpStatus status = resolveStatus(e);
        if (status.is5xxServerError()) {
            log.error("Ошибка обработки запроса: {}", e.getMessage(), e);
        } else {
            log.warn("Ошибка обработки запроса ({}): {}", status.value(), e.getMessage());
        }
        return build(status, e.getMessage(), e.getUserMessage(), exchange);
    }

    @ExceptionHandler(DataBufferLimitException.class)
    public ResponseEntity<ErrorResponseDto> handleDataBufferLimit(DataBufferLimitException e,
                                                                  ServerWebExchange exchange) {
        log.warn("Превышен допустимый размер запроса: {}", e.getMessage());
        return build(HttpStatus.PAYLOAD_TOO_LARGE, e.getMessage(), TOO_LARGE_MESSAGE, exchange);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseDto> handleResponseStatus(ResponseStatusException e,
                                                                 ServerWebExchange exchange) {
        if (hasCause(e, DataBufferLimitException.class)) {
            log.warn("Превышен допустимый размер запроса: {}", e.getMessage());
            return build(HttpStatus.PAYLOAD_TOO_LARGE, e.getMessage(), TOO_LARGE_MESSAGE, exchange);
        }
        HttpStatusCode statusCode = e.getStatusCode();
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        if (status == null) {
            status = statusCode.is5xxServerError() ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.BAD_REQUEST;
        }
        if (status.is5xxServerError()) {
            log.error("Ошибка обработки запроса: {}", e.getMessage(), e);
            return build(status, e.getMessage(), UNKNOWN_ERROR_MESSAGE, exchange);
        }
        log.warn("Ошибка запроса ({}): {}", status.value(), e.getMessage());
        String userMessage = status == HttpStatus.NOT_FOUND ? "Ресурс не найден" : BAD_REQUEST_MESSAGE;
        return build(status, e.getMessage(), userMessage, exchange);
    }

    @ExceptionHandler(DecodingException.class)
    public ResponseEntity<ErrorResponseDto> handleDecoding(DecodingException e, ServerWebExchange exchange) {
        log.warn("Не удалось разобрать тело запроса: {}", e.getMessage());
        return build(HttpStatus.BAD_REQUEST, e.getMessage(), BAD_REQUEST_MESSAGE, exchange);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleUnknown(Exception e, ServerWebExchange exchange) {
        log.error("Непредвиденная ошибка: {}", e.getMessage(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), UNKNOWN_ERROR_MESSAGE, exchange);
    }

    private HttpStatus resolveStatus(CalendarException e) {
        if (e instanceof NotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (e instanceof BadRequestException || e instanceof ValidationException) {
            return HttpStatus.BAD_REQUEST;
        }
        if (e instanceof ImpossibleOperationException) {
            return HttpStatus.UNPROCESSABLE_ENTITY;
        }
        if (e instanceof ExternalServiceException) {
            return HttpStatus.BAD_GATEWAY;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause() == current ? null : current.getCause();
        }
        return false;
    }

    private ResponseEntity<ErrorResponseDto> build(HttpStatus status, String message, String userMessage,
                                                   ServerWebExchange exchange) {
        ErrorResponseDto body = ErrorResponseDto.builder()
                .status(status.value())
                .message(message)
                .userMessage(userMessage)
                .requestId(exchange.getAttribute(ContextUtil.REQUEST_ID))
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
