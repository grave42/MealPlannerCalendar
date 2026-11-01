package com.coocking.calendar.service.external.iamcook;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import reactor.core.publisher.Mono;

/**
 * Неблокирующий ограничитель частоты исходящих запросов: не чаще одного запроса за интервал
 * (общий на экземпляр приложения). Запросы выстраиваются в очередь через {@link Mono#delay}.
 */
public class RequestRateLimiter {

    private final long intervalNanos;
    private final AtomicLong nextSlot = new AtomicLong(System.nanoTime());

    public RequestRateLimiter(Duration minInterval) {
        this.intervalNanos = minInterval.toNanos();
    }

    /**
     * Резервирует слот и завершается, когда запрос можно выполнять.
     */
    public Mono<Void> acquire() {
        return Mono.defer(() -> {
            long now = System.nanoTime();
            long slot = nextSlot.getAndUpdate(prev -> (prev - now > 0 ? prev : now) + intervalNanos);
            long delay = slot - now;
            return delay > 0 ? Mono.delay(Duration.ofNanos(delay)).then() : Mono.empty();
        });
    }
}
