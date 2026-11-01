package com.coocking.calendar.config;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import com.coocking.calendar.utils.ContextUtil;
import com.coocking.calendar.utils.RequestHeaderUtils;

/**
 * Кладёт {@code requestId} и {@code clientId} в Reactor Context на время обработки запроса.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReactiveContextFilter implements WebFilter {

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String requestId = UUID.randomUUID().toString();
        String clientId = RequestHeaderUtils.getClientId(exchange.getRequest().getHeaders());
        exchange.getAttributes().put(ContextUtil.REQUEST_ID, requestId);
        exchange.getResponse().getHeaders().set(ContextUtil.REQUEST_ID_HEADER, requestId);
        log.debug("Входящий запрос {} {}, requestId={}, clientId={}",
                exchange.getRequest().getMethod(), exchange.getRequest().getURI().getPath(), requestId, clientId);
        return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(ContextUtil.REQUEST_ID, requestId).put(ContextUtil.CLIENT_ID, clientId));
    }
}
