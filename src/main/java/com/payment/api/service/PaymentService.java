package com.payment.api.service;

import com.payment.api.dto.PaymentDTO;
import com.payment.api.entity.Payment;
import com.payment.api.repository.PaymentRepository;
import com.payment.api.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final EncryptionUtil encryptionUtil;
    private final WebhookNotificationService webhookNotificationService;

    @Transactional
    public PaymentDTO.Response createPayment(PaymentDTO.CreateRequest request) {
        log.info("Creating payment for {} {}", request.getFirstName(), request.getLastName());

        // Encrypt card number
        String encryptedCardNumber = encryptionUtil.encrypt(request.getCardNumber());
        String maskedCardNumber = encryptionUtil.maskCardNumber(request.getCardNumber());

        // Create payment entity
        Payment payment = new Payment();
        payment.setFirstName(request.getFirstName());
        payment.setLastName(request.getLastName());
        payment.setZipCode(request.getZipCode());
        payment.setCardNumberEncrypted(encryptedCardNumber);
        payment.setCardNumberMasked(maskedCardNumber);

        // Save payment
        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment created with ID: {}", savedPayment.getId());

        // Convert to response DTO
        PaymentDTO.Response response = toResponseDTO(savedPayment);

        // Trigger webhook notifications asynchronously
        webhookNotificationService.notifyWebhooks(response);

        return response;
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