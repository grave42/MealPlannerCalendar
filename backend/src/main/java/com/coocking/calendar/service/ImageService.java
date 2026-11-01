package com.coocking.calendar.service;

import java.util.Objects;

import com.coocking.calendar.exception.ValidationException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Чтение и проверка картинок блюд (размер и тип по сигнатуре содержимого).
 */
@Service
public class ImageService {

    /** Максимальный размер картинки, байт */
    public static final int MAX_IMAGE_SIZE = 5 * 1024 * 1024;

    public static final String JPEG = "image/jpeg";
    public static final String PNG = "image/png";
    public static final String WEBP = "image/webp";

    private static final String INVALID_IMAGE_USER_MESSAGE = "Допустимы изображения JPG, PNG, WEBP до 5 МБ";

    /**
     * Читает содержимое картинки, не позволяя превысить максимальный размер.
     */
    public Mono<byte[]> readBytes(Flux<DataBuffer> content) {
        return DataBufferUtils.join(content, MAX_IMAGE_SIZE + 1)
                .map(buffer -> {
                    try {
                        byte[] bytes = new byte[buffer.readableByteCount()];
                        buffer.read(bytes);
                        return bytes;
                    } finally {
                        DataBufferUtils.release(buffer);
                    }
                })
                .onErrorMap(DataBufferLimitException.class, e -> new ValidationException(
                        "Image is larger than " + MAX_IMAGE_SIZE + " bytes", INVALID_IMAGE_USER_MESSAGE));
    }

    /**
     * Проверяет размер и тип картинки по сигнатуре и возвращает определённый тип содержимого.
     *
     * @throws ValidationException если картинка пустая, слишком большая или неподдерживаемого типа
     */
    public String detectContentType(byte[] content) {
        if (Objects.isNull(content) || content.length == 0) {
            throw new ValidationException("Image is empty", INVALID_IMAGE_USER_MESSAGE);
        }
        if (content.length > MAX_IMAGE_SIZE) {
            throw new ValidationException("Image is larger than " + MAX_IMAGE_SIZE + " bytes",
                    INVALID_IMAGE_USER_MESSAGE);
        }
        if (isJpeg(content)) {
            return JPEG;
        }
        if (isPng(content)) {
            return PNG;
        }
        if (isWebp(content)) {
            return WEBP;
        }
        throw new ValidationException("Unsupported image type", INVALID_IMAGE_USER_MESSAGE);
    }

    private boolean isJpeg(byte[] c) {
        return c.length > 3 && (c[0] & 0xFF) == 0xFF && (c[1] & 0xFF) == 0xD8 && (c[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] c) {
        byte[] signature = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        if (c.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (c[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean isWebp(byte[] c) {
        return c.length > 12
                && c[0] == 'R' && c[1] == 'I' && c[2] == 'F' && c[3] == 'F'
                && c[8] == 'W' && c[9] == 'E' && c[10] == 'B' && c[11] == 'P';
    }
}
