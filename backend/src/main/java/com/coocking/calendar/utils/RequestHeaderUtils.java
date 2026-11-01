package com.coocking.calendar.utils;

import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * Разбор заголовков запроса.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RequestHeaderUtils {

    /** Максимальная длина идентификатора клиента */
    private static final int MAX_CLIENT_ID_LENGTH = 100;

    /**
     * Возвращает идентификатор клиента из заголовка {@code X-Client-Id} или {@code anonymous}.
     */
    public static String getClientId(MultiValueMap<String, String> headers) {
        String value = getFirst(headers, ContextUtil.CLIENT_ID_HEADER);
        if (!StringUtils.hasText(value)) {
            return ContextUtil.ANONYMOUS_CLIENT;
        }
        String trimmed = value.trim();
        return trimmed.length() > MAX_CLIENT_ID_LENGTH ? trimmed.substring(0, MAX_CLIENT_ID_LENGTH) : trimmed;
    }

    /**
     * Возвращает первое значение заголовка (без учёта регистра имени) или {@code null}.
     */
    public static String getFirst(MultiValueMap<String, String> headers, String name) {
        for (String key : headers.keySet()) {
            if (key.equalsIgnoreCase(name)) {
                List<String> values = headers.get(key);
                return (values == null || values.isEmpty()) ? null : values.get(0);
            }
        }
        return null;
    }
}
