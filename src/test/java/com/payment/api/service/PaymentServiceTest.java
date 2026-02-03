package com.payment.api.service;

import com.payment.api.dto.PaymentDTO;
import com.payment.api.entity.Payment;
import com.payment.api.repository.PaymentRepository;
import com.payment.api.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private EncryptionUtil encryptionUtil;

    @Mock
    private WebhookNotificationService webhookNotificationService;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentDTO.CreateRequest createRequest;
    private Payment savedPayment;

    @BeforeEach
    void setUp() {
        createRequest = new PaymentDTO.CreateRequest();
        createRequest.setFirstName("John");
        createRequest.setLastName("Doe");
        createRequest.setZipCode("12345");
        createRequest.setCardNumber("4532015112830366");

        savedPayment = new Payment();
        savedPayment.setId(1L);
        savedPayment.setFirstName("John");
        savedPayment.setLastName("Doe");
        savedPayment.setZipCode("12345");
        savedPayment.setCardNumberEncrypted("encrypted123");
        savedPayment.setCardNumberMasked("****0366");
        savedPayment.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void createPayment_Success() {
        // Arrange
        when(encryptionUtil.encrypt(anyString())).thenReturn("encrypted123");
        when(encryptionUtil.maskCardNumber(anyString())).thenReturn("****0366");
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        // Act
        PaymentDTO.Response response = paymentService.createPayment(createRequest);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertEquals("12345", response.getZipCode());
        assertEquals("****0366", response.getCardNumberMasked());
        assertNotNull(response.getCreatedAt());

        verify(encryptionUtil).encrypt("4532015112830366");
        verify(encryptionUtil).maskCardNumber("4532015112830366");
        verify(paymentRepository).save(any(Payment.class));
        verify(webhookNotificationService).notifyWebhooks(any(PaymentDTO.Response.class));
    }

    @Test
    void createPayment_EncryptsCardNumber() {
        // Arrange
        when(encryptionUtil.encrypt(anyString())).thenReturn("encrypted123");
        when(encryptionUtil.maskCardNumber(anyString())).thenReturn("****0366");
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        // Act
        paymentService.createPayment(createRequest);

        // Assert
        verify(encryptionUtil, times(1)).encrypt("4532015112830366");
    }

    @Test
    void createPayment_TriggersWebhookNotification() {
        // Arrange
        when(encryptionUtil.encrypt(anyString())).thenReturn("encrypted123");
        when(encryptionUtil.maskCardNumber(anyString())).thenReturn("****0366");
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        // Act
        paymentService.createPayment(createRequest);

        // Assert
        verify(webhookNotificationService, times(1)).notifyWebhooks(any(PaymentDTO.Response.class));
    }
}