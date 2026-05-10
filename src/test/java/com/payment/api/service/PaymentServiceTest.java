package com.payment.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private EncryptionUtil encryptionUtil;

    @Mock
    private WebhookNotificationService webhookNotificationService;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private ObjectMapper objectMapper;

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
        savedPayment.setCardNumberMasked("************0366");
        savedPayment.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void createPayment_Success() {
        when(encryptionUtil.encrypt(anyString())).thenReturn("encrypted123");
        when(encryptionUtil.maskCardNumber(anyString())).thenReturn("************0366");
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        PaymentDTO.Response response = paymentService.createPayment(createRequest, null);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertEquals("12345", response.getZipCode());
        assertEquals("************0366", response.getCardNumberMasked());
        assertNotNull(response.getCreatedAt());

        verify(encryptionUtil).encrypt("4532015112830366");
        verify(encryptionUtil).maskCardNumber("4532015112830366");
        verify(paymentRepository).save(any(Payment.class));
        verify(webhookNotificationService).notifyWebhooks(any(PaymentDTO.Response.class));
    }

    @Test
    void createPayment_EncryptsCardNumber() {
        when(encryptionUtil.encrypt(anyString())).thenReturn("encrypted123");
        when(encryptionUtil.maskCardNumber(anyString())).thenReturn("************0366");
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        paymentService.createPayment(createRequest, null);

        verify(encryptionUtil, times(1)).encrypt("4532015112830366");
    }

    @Test
    void createPayment_TriggersWebhookNotification() {
        when(encryptionUtil.encrypt(anyString())).thenReturn("encrypted123");
        when(encryptionUtil.maskCardNumber(anyString())).thenReturn("************0366");
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        paymentService.createPayment(createRequest, null);

        verify(webhookNotificationService, times(1)).notifyWebhooks(any(PaymentDTO.Response.class));
    }

    @Test
    void createPayment_WithIdempotencyKey_ReturnsCachedResponse() throws Exception {
        PaymentDTO.Response cachedResponse = new PaymentDTO.Response(
            99L, "John", "Doe", "12345", "************0366", LocalDateTime.now()
        );
        when(idempotencyService.getCachedResponse("key-123"))
            .thenReturn(Optional.of("{\"id\":99}"));
        when(objectMapper.readValue("{\"id\":99}", PaymentDTO.Response.class))
            .thenReturn(cachedResponse);

        PaymentDTO.Response response = paymentService.createPayment(createRequest, "key-123");

        assertEquals(99L, response.getId());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createPayment_WithNewIdempotencyKey_ProcessesAndStores() throws Exception {
        when(idempotencyService.getCachedResponse("new-key")).thenReturn(Optional.empty());
        when(idempotencyService.tryReserve("new-key")).thenReturn(true);
        when(encryptionUtil.encrypt(anyString())).thenReturn("encrypted123");
        when(encryptionUtil.maskCardNumber(anyString())).thenReturn("************0366");
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"id\":1}");

        PaymentDTO.Response response = paymentService.createPayment(createRequest, "new-key");

        assertNotNull(response);
        verify(idempotencyService).complete(eq("new-key"), anyString());
    }
}
