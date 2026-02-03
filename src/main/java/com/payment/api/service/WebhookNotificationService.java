package com.payment.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookNotificationService {

    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;

    @Value("${webhook.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${webhook.retry.initial-delay:5000}")
    private long initialRetryDelay;

    @Value("${webhook.timeout.connect:5000}")
    private int connectTimeout;

    @Value("${webhook.timeout.read:10000}")
    private int readTimeout;

    /**
     * Asynchronously notify all active webhooks about a new payment
     */
    @Async
    public void notifyWebhooks(PaymentDTO.Response payment) {
        log.info("Starting webhook notifications for payment ID: {}", payment.getId());

        List<Webhook> activeWebhooks = webhookService.getActiveWebhooks();
        
        if (activeWebhooks.isEmpty()) {
            log.info("No active webhooks to notify");
            return;
        }

        WebhookEventDTO event = new WebhookEventDTO(
            "PAYMENT_CREATED",
            LocalDateTime.now(),
            payment
        );

        for (Webhook webhook : activeWebhooks) {
            notifyWebhookWithRetry(webhook, event);
        }
    }

    /**
     * Notify a single webhook with retry mechanism
     */
    private void notifyWebhookWithRetry(Webhook webhook, WebhookEventDTO event) {
        int attempt = 0;
        boolean success = false;

        while (attempt < maxRetryAttempts && !success) {
            attempt++;
            try {
                log.info("Attempting to notify webhook {} (attempt {}/{})", 
                    webhook.getUrl(), attempt, maxRetryAttempts);

                sendWebhookNotification(webhook.getUrl(), event);
                success = true;
                log.info("Successfully notified webhook: {}", webhook.getUrl());

            } catch (Exception e) {
                log.error("Failed to notify webhook {} (attempt {}/{}): {}", 
                    webhook.getUrl(), attempt, maxRetryAttempts, e.getMessage());

                if (attempt < maxRetryAttempts) {
                    long delay = calculateRetryDelay(attempt);
                    log.info("Retrying after {} ms", delay);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Retry interrupted for webhook: {}", webhook.getUrl());
                        break;
                    }
                } else {
                    log.error("Max retry attempts reached for webhook: {}", webhook.getUrl());
                }
            }
        }
    }

    /**
     * Send HTTP POST request to webhook endpoint
     */
    private void sendWebhookNotification(String webhookUrl, WebhookEventDTO event) throws Exception {
        URL url = new URL(webhookUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "PaymentWebhookAPI/1.0");
            connection.setDoOutput(true);
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);

            // Write JSON payload
            String jsonPayload = objectMapper.writeValueAsString(event);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Check response code
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new RuntimeException("Webhook returned status code: " + responseCode);
            }

        } finally {
            connection.disconnect();
        }
    }

    /**
     * Calculate exponential backoff delay
     */
    private long calculateRetryDelay(int attempt) {
        return initialRetryDelay * (long) Math.pow(2, attempt - 1);
    }
}