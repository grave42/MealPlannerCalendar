package com.coocking.calendar.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Ключи Reactor Context и значения по умолчанию.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContextUtil {

    /** Ключ идентификатора запроса */
    public static final String REQUEST_ID = "requestId";

    /** Ключ идентификатора клиента */
    public static final String CLIENT_ID = "clientId";

    /** Идентификатор клиента, если заголовок не передан */
    public static final String ANONYMOUS_CLIENT = "anonymous";

    /** Заголовок с идентификатором клиента */
    public static final String CLIENT_ID_HEADER = "X-Client-Id";

    /** Заголовок с идентификатором запроса */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
}
