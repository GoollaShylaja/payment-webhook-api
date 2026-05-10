package com.payment.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.api.circuitbreaker.CircuitBreakerRegistry;
import com.payment.api.circuitbreaker.WebhookCircuitBreaker;
import com.payment.api.dto.PaymentDTO;
import com.payment.api.dto.WebhookEventDTO;
import com.payment.api.entity.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookNotificationService {

    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Value("${webhook.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${webhook.retry.initial-delay:5000}")
    private long initialRetryDelay;

    @Value("${webhook.timeout.connect:5000}")
    private int connectTimeout;

    @Value("${webhook.timeout.read:10000}")
    private int readTimeout;

    @Async("webhookExecutor")
    public void notifyWebhooks(PaymentDTO.Response payment) {
        log.info("Starting webhook notifications for payment ID: {}", payment.getId());

        List<Webhook> activeWebhooks = webhookService.getActiveWebhooks();
        if (activeWebhooks.isEmpty()) {
            log.info("No active webhooks to notify");
            return;
        }

        WebhookEventDTO event = new WebhookEventDTO("PAYMENT_CREATED", LocalDateTime.now(), payment);

        for (Webhook webhook : activeWebhooks) {
            notifyWebhookWithCircuitBreaker(webhook, event);
        }
    }

    private void notifyWebhookWithCircuitBreaker(Webhook webhook, WebhookEventDTO event) {
        WebhookCircuitBreaker circuitBreaker = circuitBreakerRegistry.getOrCreate(webhook.getUrl());

        if (!circuitBreaker.allowRequest()) {
            log.warn("Circuit breaker OPEN — skipping webhook: {} (state={})",
                webhook.getUrl(), circuitBreaker.getState());
            return;
        }

        boolean success = false;
        int attempt = 0;

        while (attempt < maxRetryAttempts && !success) {
            attempt++;
            try {
                log.info("Notifying webhook {} (attempt {}/{}, circuit={})",
                    webhook.getUrl(), attempt, maxRetryAttempts, circuitBreaker.getState());

                sendWebhookNotification(webhook.getUrl(), event);
                success = true;
                circuitBreaker.recordSuccess();
                log.info("Webhook notified successfully: {}", webhook.getUrl());

            } catch (Exception e) {
                log.error("Webhook notification failed {} (attempt {}/{}): {}",
                    webhook.getUrl(), attempt, maxRetryAttempts, e.getMessage());

                if (attempt < maxRetryAttempts) {
                    long delay = calculateRetryDelay(attempt);
                    log.info("Retrying webhook {} after {} ms", webhook.getUrl(), delay);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    circuitBreaker.recordFailure();
                    log.error("All {} attempts exhausted for webhook: {} (circuit={})",
                        maxRetryAttempts, webhook.getUrl(), circuitBreaker.getState());
                }
            }
        }
    }

    private void validateWebhookUrl(String webhookUrl) throws Exception {
        URL url = new URL(webhookUrl);
        InetAddress address = InetAddress.getByName(url.getHost());
        if (address.isLoopbackAddress() || address.isSiteLocalAddress()
                || address.isLinkLocalAddress() || address.isAnyLocalAddress()) {
            throw new SecurityException("Webhook URL resolves to a private/internal address: " + webhookUrl);
        }
    }

    private void sendWebhookNotification(String webhookUrl, WebhookEventDTO event) throws Exception {
        validateWebhookUrl(webhookUrl);
        URL url = new URL(webhookUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "PaymentWebhookAPI/1.0");
            connection.setDoOutput(true);
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);

            String jsonPayload = objectMapper.writeValueAsString(event);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new RuntimeException("Webhook returned non-2xx status: " + responseCode);
            }
        } finally {
            connection.disconnect();
        }
    }

    private long calculateRetryDelay(int attempt) {
        return initialRetryDelay * (long) Math.pow(2, attempt - 1);
    }
}
