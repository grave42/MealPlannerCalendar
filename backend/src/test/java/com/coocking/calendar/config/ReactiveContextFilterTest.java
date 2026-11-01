package com.coocking.calendar.config;

import java.util.concurrent.atomic.AtomicReference;

import com.coocking.calendar.utils.ContextUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Класс ReactiveContextFilter")
class ReactiveContextFilterTest {

    private final ReactiveContextFilter filter = new ReactiveContextFilter();

    private String runAndCaptureClientId(MockServerHttpRequest request, AtomicReference<String> requestId) {
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<String> clientId = new AtomicReference<>();
        WebFilterChain chain = e -> Mono.deferContextual(ctx -> {
            clientId.set(ctx.get(ContextUtil.CLIENT_ID));
            requestId.set(ctx.get(ContextUtil.REQUEST_ID));
            return Mono.empty();
        });

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(requestId.get(), exchange.getResponse().getHeaders().getFirst(ContextUtil.REQUEST_ID_HEADER));
        return clientId.get();
    }

    @Test
    @DisplayName("clientId берётся из заголовка X-Client-Id")
    void testClientIdFromHeader() {
        AtomicReference<String> requestId = new AtomicReference<>();

        String clientId = runAndCaptureClientId(
                MockServerHttpRequest.get("/x").header("X-Client-Id", "mom").build(), requestId);

        assertEquals("mom", clientId);
        assertNotNull(requestId.get());
    }

    @Test
    @DisplayName("Без заголовка clientId = anonymous")
    void testAnonymous() {
        AtomicReference<String> requestId = new AtomicReference<>();

        assertEquals("anonymous", runAndCaptureClientId(MockServerHttpRequest.get("/x").build(), requestId));
    }
}
