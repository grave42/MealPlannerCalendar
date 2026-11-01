package com.coocking.calendar.exception.handler;

import java.util.stream.Stream;

import com.coocking.calendar.exception.BadRequestException;
import com.coocking.calendar.exception.CalendarException;
import com.coocking.calendar.exception.ExternalServiceException;
import com.coocking.calendar.exception.ImpossibleOperationException;
import com.coocking.calendar.exception.NotFoundException;
import com.coocking.calendar.exception.ValidationException;
import com.coocking.calendar.model.exception.ErrorResponseDto;
import com.coocking.calendar.utils.ContextUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Класс GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private ServerWebExchange createExchange() {
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/test"));
        exchange.getAttributes().put(ContextUtil.REQUEST_ID, "req-1");
        return exchange;
    }

    static Stream<Arguments> calendarExceptions() {
        return Stream.of(
                Arguments.of(new NotFoundException("nf", "Не найдено"), HttpStatus.NOT_FOUND),
                Arguments.of(new BadRequestException("br", "Плохой запрос"), HttpStatus.BAD_REQUEST),
                Arguments.of(new ValidationException("v", "Не прошло проверку"), HttpStatus.BAD_REQUEST),
                Arguments.of(new ImpossibleOperationException("io", "Невозможно"), HttpStatus.UNPROCESSABLE_ENTITY),
                Arguments.of(new ExternalServiceException("es", "Сервис недоступен"), HttpStatus.BAD_GATEWAY),
                Arguments.of(new CalendarException("ce", "Ошибка"), HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("calendarExceptions")
    @DisplayName("Исключения приложения отображаются в HTTP-статусы с userMessage и requestId")
    void testCalendarExceptions(CalendarException exception, HttpStatus expected) {
        ResponseEntity<ErrorResponseDto> response = handler.handleCalendarException(exception, createExchange());

        assertEquals(expected, response.getStatusCode());
        ErrorResponseDto body = response.getBody();
        assertNotNull(body);
        assertEquals(expected.value(), body.getStatus());
        assertEquals(exception.getMessage(), body.getMessage());
        assertEquals(exception.getUserMessage(), body.getUserMessage());
        assertEquals("req-1", body.getRequestId());
        assertNotNull(body.getTimestamp());
    }

    @Test
    @DisplayName("Превышение размера буфера даёт 413")
    void testDataBufferLimit() {
        ResponseEntity<ErrorResponseDto> response =
                handler.handleDataBufferLimit(new DataBufferLimitException("limit"), createExchange());

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertTrue(response.getBody().getUserMessage().contains("6 МБ"));
    }

    @Test
    @DisplayName("Ошибка запроса, вызванная превышением размера, даёт 413")
    void testWrappedDataBufferLimit() {
        ServerWebInputException exception =
                new ServerWebInputException("Failed to read", null, new DataBufferLimitException("limit"));

        ResponseEntity<ErrorResponseDto> response = handler.handleResponseStatus(exception, createExchange());

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
    }

    @Test
    @DisplayName("ServerWebInputException даёт 400")
    void testServerWebInput() {
        ResponseEntity<ErrorResponseDto> response =
                handler.handleResponseStatus(new ServerWebInputException("bad"), createExchange());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Некорректный запрос", response.getBody().getUserMessage());
    }

    @Test
    @DisplayName("Неизвестный ресурс даёт 404")
    void testResponseStatusNotFound() {
        ResponseEntity<ErrorResponseDto> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.NOT_FOUND), createExchange());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Ресурс не найден", response.getBody().getUserMessage());
    }

    @Test
    @DisplayName("ResponseStatusException 5xx даёт 5xx со стандартным сообщением")
    void testResponseStatus5xx() {
        ResponseEntity<ErrorResponseDto> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE), createExchange());

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    @DisplayName("Ошибка разбора тела даёт 400")
    void testDecoding() {
        ResponseEntity<ErrorResponseDto> response =
                handler.handleDecoding(new DecodingException("bad json"), createExchange());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Прочие исключения дают 500 без раскрытия деталей пользователю")
    void testUnknown() {
        ResponseEntity<ErrorResponseDto> response =
                handler.handleUnknown(new IllegalStateException("secret details"), createExchange());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Внутренняя ошибка сервера. Попробуйте позже", response.getBody().getUserMessage());
    }
}
