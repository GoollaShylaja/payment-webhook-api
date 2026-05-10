package com.payment.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.api.dto.PaymentDTO;
import com.payment.api.entity.Payment;
import com.payment.api.repository.PaymentRepository;
import com.payment.api.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final EncryptionUtil encryptionUtil;
    private final WebhookNotificationService webhookNotificationService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Transactional
    public PaymentDTO.Response createPayment(PaymentDTO.CreateRequest request, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return createPaymentWithIdempotency(request, idempotencyKey);
        }
        return processPayment(request);
    }

    private PaymentDTO.Response createPaymentWithIdempotency(PaymentDTO.CreateRequest request,
                                                              String idempotencyKey) {
        Optional<String> cached = idempotencyService.getCachedResponse(idempotencyKey);
        if (cached.isPresent()) {
            log.info("Returning cached response for idempotency key: {}", idempotencyKey);
            return deserializeResponse(cached.get());
        }

        boolean reserved = idempotencyService.tryReserve(idempotencyKey);
        if (!reserved) {
            // Concurrent request with same key — wait briefly and return cached result
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return idempotencyService.getCachedResponse(idempotencyKey)
                .map(this::deserializeResponse)
                .orElseThrow(() -> new IllegalStateException(
                    "Concurrent request detected for idempotency key: " + idempotencyKey));
        }

        try {
            PaymentDTO.Response response = processPayment(request);
            idempotencyService.complete(idempotencyKey, serializeResponse(response));
            return response;
        } catch (Exception e) {
            idempotencyService.release(idempotencyKey);
            throw e;
        }
    }

    private PaymentDTO.Response processPayment(PaymentDTO.CreateRequest request) {
        log.info("Creating payment for {} {}", request.getFirstName(), request.getLastName());

        String encryptedCardNumber = encryptionUtil.encrypt(request.getCardNumber());
        String maskedCardNumber = encryptionUtil.maskCardNumber(request.getCardNumber());

        Payment payment = new Payment();
        payment.setFirstName(request.getFirstName());
        payment.setLastName(request.getLastName());
        payment.setZipCode(request.getZipCode());
        payment.setCardNumberEncrypted(encryptedCardNumber);
        payment.setCardNumberMasked(maskedCardNumber);

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment created with ID: {}", savedPayment.getId());

        PaymentDTO.Response response = toResponseDTO(savedPayment);
        webhookNotificationService.notifyWebhooks(response);
        return response;
    }

    private String serializeResponse(PaymentDTO.Response response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize payment response", e);
        }
    }

    private PaymentDTO.Response deserializeResponse(String json) {
        try {
            return objectMapper.readValue(json, PaymentDTO.Response.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize cached payment response", e);
        }
    }

    private PaymentDTO.Response toResponseDTO(Payment payment) {
        return new PaymentDTO.Response(
            payment.getId(),
            payment.getFirstName(),
            payment.getLastName(),
            payment.getZipCode(),
            payment.getCardNumberMasked(),
            payment.getCreatedAt()
        );
    }
}
