package com.payment.api.controller;

import com.payment.api.dto.ErrorResponse;
import com.payment.api.dto.PaymentDTO;
import com.payment.api.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Validated
@Tag(name = "Payments", description = "Payment management APIs")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(
        summary = "Create a new payment",
        description = "Creates a new payment with encrypted card information and triggers webhook notifications"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Payment created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaymentDTO.Response.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 1,
                        "firstName": "John",
                        "lastName": "Doe",
                        "zipCode": "12345",
                        "cardNumberMasked": "****0366",
                        "createdAt": "2026-02-02T10:30:00"
                    }
                """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {
                        "timestamp": "2026-02-02T10:30:00",
                        "status": 400,
                        "error": "Validation Failed",
                        "message": "Invalid input data",
                        "path": "/api/payments",
                        "details": [
                            "firstName: First name must contain only letters, spaces, hyphens, and apostrophes",
                            "cardNumber: Card number must contain only digits and be between 13-19 characters long"
                        ]
                    }
                """)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {
                        "timestamp": "2026-02-02T10:30:00",
                        "status": 500,
                        "error": "Internal Server Error",
                        "message": "An unexpected error occurred",
                        "path": "/api/payments"
                    }
                """)
            )
        )
    })
    public ResponseEntity<PaymentDTO.Response> createPayment(
            @Valid @RequestBody PaymentDTO.CreateRequest request) {
        
        PaymentDTO.Response response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}