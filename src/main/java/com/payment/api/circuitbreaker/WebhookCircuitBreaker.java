package com.payment.api.circuitbreaker;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class WebhookCircuitBreaker {

    @Getter
    private final String webhookUrl;
    private final int failureThreshold;
    private final int successThreshold;
    private final long waitDurationMs;

    private final AtomicReference<CircuitBreakerState> state =
        new AtomicReference<>(CircuitBreakerState.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicLong openedAt = new AtomicLong(0);

    public WebhookCircuitBreaker(String webhookUrl, int failureThreshold,
                                  int successThreshold, long waitDurationMs) {
        this.webhookUrl = webhookUrl;
        this.failureThreshold = failureThreshold;
        this.successThreshold = successThreshold;
        this.waitDurationMs = waitDurationMs;
    }

    public boolean allowRequest() {
        CircuitBreakerState current = state.get();
        switch (current) {
            case CLOSED:
                return true;
            case OPEN:
                if (System.currentTimeMillis() - openedAt.get() >= waitDurationMs) {
                    if (state.compareAndSet(CircuitBreakerState.OPEN, CircuitBreakerState.HALF_OPEN)) {
                        successCount.set(0);
                        log.info("Circuit breaker HALF_OPEN for webhook: {}", webhookUrl);
                    }
                    return true;
                }
                return false;
            case HALF_OPEN:
                return true;
            default:
                return false;
        }
    }

    public void recordSuccess() {
        CircuitBreakerState current = state.get();
        if (current == CircuitBreakerState.HALF_OPEN) {
            if (successCount.incrementAndGet() >= successThreshold) {
                state.set(CircuitBreakerState.CLOSED);
                failureCount.set(0);
                successCount.set(0);
                log.info("Circuit breaker CLOSED (recovered) for webhook: {}", webhookUrl);
            }
        } else if (current == CircuitBreakerState.CLOSED) {
            failureCount.set(0);
        }
    }

    public void recordFailure() {
        CircuitBreakerState current = state.get();
        if (current == CircuitBreakerState.HALF_OPEN) {
            state.set(CircuitBreakerState.OPEN);
            openedAt.set(System.currentTimeMillis());
            successCount.set(0);
            log.warn("Circuit breaker re-OPENED after probe failure for webhook: {}", webhookUrl);
        } else if (current == CircuitBreakerState.CLOSED) {
            int failures = failureCount.incrementAndGet();
            if (failures >= failureThreshold) {
                state.set(CircuitBreakerState.OPEN);
                openedAt.set(System.currentTimeMillis());
                log.warn("Circuit breaker OPENED for webhook: {} after {} failures", webhookUrl, failures);
            }
        }
    }

    public CircuitBreakerState getState() {
        return state.get();
    }

    public int getFailureCount() {
        return failureCount.get();
    }
}
