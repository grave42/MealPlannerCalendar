package com.coocking.calendar.service;

import com.coocking.calendar.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Класс ImageService")
class ImageServiceTest {

    private final ImageService imageService = new ImageService();

    @Test
    @DisplayName("Тип определяется по сигнатуре содержимого")
    void testDetectContentType() {
        assertEquals("image/jpeg", imageService.detectContentType(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}));
        assertEquals("image/png",
                imageService.detectContentType(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}));
        assertEquals("image/webp", imageService.detectContentType(
                new byte[]{'R', 'I', 'F', 'F', 1, 0, 0, 0, 'W', 'E', 'B', 'P', 'V'}));
    }

    @Test
    @DisplayName("Пустой файл, GIF и текст отклоняются")
    void testRejected() {
        assertThrows(ValidationException.class, () -> imageService.detectContentType(new byte[0]));
        assertThrows(ValidationException.class, () -> imageService.detectContentType(null));
        assertThrows(ValidationException.class, () -> imageService.detectContentType("GIF89a....".getBytes()));
        assertThrows(ValidationException.class, () -> imageService.detectContentType("<html>".getBytes()));
    }

    @Test
    @DisplayName("Файл больше 5 МБ отклоняется")
    void testTooLarge() {
        byte[] big = new byte[ImageService.MAX_IMAGE_SIZE + 1];
        big[0] = (byte) 0xFF;
        big[1] = (byte) 0xD8;
        big[2] = (byte) 0xFF;

        ValidationException e = assertThrows(ValidationException.class, () -> imageService.detectContentType(big));
        assertEquals("Допустимы изображения JPG, PNG, WEBP до 5 МБ", e.getUserMessage());
    }

    @Test
    @DisplayName("Чтение байтов из потока буферов")
    void testReadBytes() {
        DefaultDataBufferFactory factory = DefaultDataBufferFactory.sharedInstance;

        StepVerifier.create(imageService.readBytes(Flux.just(factory.wrap(new byte[]{1, 2}), factory.wrap(new byte[]{3}))))
                .assertNext(bytes -> assertEquals(3, bytes.length))
                .verifyComplete();
    }

    @Test
    @DisplayName("Слишком большой поток отклоняется при чтении")
    void testReadBytesTooLarge() {
        DefaultDataBufferFactory factory = DefaultDataBufferFactory.sharedInstance;

        StepVerifier.create(imageService.readBytes(Flux.just(factory.wrap(new byte[ImageService.MAX_IMAGE_SIZE + 2]))))
                .expectError(ValidationException.class)
                .verify();
    }
}
