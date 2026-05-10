package com.payment.api.circuitbreaker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class CircuitBreakerRegistry {

    @Value("${circuit-breaker.failure-threshold:5}")
    private int failureThreshold;

    @Value("${circuit-breaker.success-threshold:2}")
    private int successThreshold;

    @Value("${circuit-breaker.wait-duration-ms:60000}")
    private long waitDurationMs;

    private final ConcurrentHashMap<String, WebhookCircuitBreaker> breakers = new ConcurrentHashMap<>();

    public WebhookCircuitBreaker getOrCreate(String webhookUrl) {
        return breakers.computeIfAbsent(webhookUrl, url ->
            new WebhookCircuitBreaker(url, failureThreshold, successThreshold, waitDurationMs));
    }

    public Collection<WebhookCircuitBreaker> getAll() {
        return breakers.values();
    }
}
