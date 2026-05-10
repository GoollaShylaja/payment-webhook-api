package com.payment.api.controller;

import com.payment.api.config.OpenAPIExamples;
import com.payment.api.dto.ErrorResponse;
import com.payment.api.dto.PaymentDTO;
import com.payment.api.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
        description = "Creates a new payment with encrypted card information and triggers webhook notifications. "
            + "Supply an `Idempotency-Key` header to safely retry the request — duplicate keys return "
            + "the original response without creating a second payment.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaymentDTO.CreateRequest.class),
                examples = {
                    @ExampleObject(
                        name = "Valid Payment",
                        summary = "Valid payment example",
                        value = OpenAPIExamples.PAYMENT_REQUEST_EXAMPLE
                    ),
                    @ExampleObject(
                        name = "With Extended Zip",
                        summary = "Payment with ZIP+4 format",
                        value = OpenAPIExamples.EXTENDED_ZIP_PAYMENT_REQUEST_EXAMPLE
                    )
                }
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Payment created successfully (or idempotent replay of prior creation)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaymentDTO.Response.class),
                examples = @ExampleObject(value = OpenAPIExamples.PAYMENT_RESPONSE_SUCCESS)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = OpenAPIExamples.PAYMENT_ERROR_400_EXAMPLE)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = OpenAPIExamples.PAYMENT_ERROR_500_EXAMPLE)
            )
        )
    })
    public ResponseEntity<PaymentDTO.Response> createPayment(
            @Parameter(
                description = "Unique key to ensure idempotency. Re-sending the same key returns the "
                    + "original response without creating a duplicate payment. Keys expire after 24 hours.",
                example = "a3f1c2e4-7b8d-4e5f-9a0b-1c2d3e4f5a6b"
            )
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PaymentDTO.CreateRequest request) {

        PaymentDTO.Response response = paymentService.createPayment(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
