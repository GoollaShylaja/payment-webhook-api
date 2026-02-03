package com.payment.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.api.dto.PaymentDTO;
import com.payment.api.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @Test
    void createPayment_ValidRequest_ReturnsCreated() throws Exception {
        // Arrange
        PaymentDTO.CreateRequest request = new PaymentDTO.CreateRequest(
            "John", "Doe", "12345", "4532015112830366"
        );

        PaymentDTO.Response response = new PaymentDTO.Response(
            1L, "John", "Doe", "12345", "****0366", LocalDateTime.now()
        );

        when(paymentService.createPayment(any(PaymentDTO.CreateRequest.class)))
            .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.firstName").value("John"))
            .andExpect(jsonPath("$.lastName").value("Doe"))
            .andExpect(jsonPath("$.zipCode").value("12345"))
            .andExpect(jsonPath("$.cardNumberMasked").value("****0366"));
    }

    @Test
    void createPayment_MissingFirstName_ReturnsBadRequest() throws Exception {
        // Arrange
        PaymentDTO.CreateRequest request = new PaymentDTO.CreateRequest(
            "", "Doe", "12345", "4532015112830366"
        );

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_InvalidCardNumber_ReturnsBadRequest() throws Exception {
        // Arrange
        PaymentDTO.CreateRequest request = new PaymentDTO.CreateRequest(
            "John", "Doe", "12345", "123"  // Too short
        );

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_InvalidZipCode_ReturnsBadRequest() throws Exception {
        // Arrange
        PaymentDTO.CreateRequest request = new PaymentDTO.CreateRequest(
            "John", "Doe", "ABCDE", "4532015112830366"  // Invalid zip
        );

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
    @Test
    void createPayment_FirstNameWithNumbers_ReturnsBadRequest() throws Exception {
        // Arrange
        PaymentDTO.CreateRequest request = new PaymentDTO.CreateRequest(
            "John123", "Doe", "12345", "4532015112830366"  // Numbers in first name
        );

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid input data"));
    }

    @Test
    void createPayment_LastNameWithNumbers_ReturnsBadRequest() throws Exception {
        // Arrange
        PaymentDTO.CreateRequest request = new PaymentDTO.CreateRequest(
            "John", "Doe456", "12345", "4532015112830366"  // Numbers in last name
        );

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_FirstNameWithSpecialCharacters_ReturnsBadRequest() throws Exception {
        // Arrange
        PaymentDTO.CreateRequest request = new PaymentDTO.CreateRequest(
            "John@Smith", "Doe", "12345", "4532015112830366"  // Invalid special char
        );

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_ValidNameWithHyphen_ReturnsCreated() throws Exception {
        // Arrange
        PaymentDTO.CreateRequest request = new PaymentDTO.CreateRequest(
            "Mary-Jane", "Smith-Jones", "12345", "4532015112830366"  // Valid hyphens
        );

        PaymentDTO.Response response = new PaymentDTO.Response(
            1L, "Mary-Jane", "Smith-Jones", "12345", "****0366", LocalDateTime.now()
        );

        
        when(paymentService.createPayment(any(PaymentDTO.CreateRequest.class)))
            .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.firstName").value("Mary-Jane"))
            .andExpect(jsonPath("$.lastName").value("Smith-Jones"));
    }

    @Test
    void createPayment_ValidNameWithApostrophe_ReturnsCreated() throws Exception {
        // Arrange
        PaymentDTO.CreateRequest request = new PaymentDTO.CreateRequest(
            "O'Brien", "D'Angelo", "12345", "4532015112830366"  // Valid apostrophes
        );

        PaymentDTO.Response response = new PaymentDTO.Response(
            1L, "O'Brien", "D'Angelo", "12345", "****0366", LocalDateTime.now()
        );

        when(paymentService.createPayment(any(PaymentDTO.CreateRequest.class)))
            .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.firstName").value("O'Brien"))
            .andExpect(jsonPath("$.lastName").value("D'Angelo"));
    }
}