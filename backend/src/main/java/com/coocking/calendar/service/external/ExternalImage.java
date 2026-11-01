package com.coocking.calendar.service.external;

import lombok.Builder;
import lombok.Value;

/**
 * Картинка, скачанная из внешнего источника.
 */
@Value
@Builder
public class ExternalImage {

    /** Содержимое */
    byte[] content;

    /** Тип содержимого из ответа источника */
    String contentType;
}
