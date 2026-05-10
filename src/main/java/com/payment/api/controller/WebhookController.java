package com.payment.api.controller;

import com.payment.api.config.OpenAPIExamples;
import com.payment.api.dto.ErrorResponse;
import com.payment.api.dto.WebhookDTO;
import com.payment.api.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import java.util.List;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Validated
@Tag(name = "Webhooks", description = "Webhook management APIs")
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping
    @Operation(
        summary = "Register a new webhook",
        description = "Register a webhook endpoint that will receive payment notifications",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = WebhookDTO.CreateRequest.class),
                examples = {
                    @ExampleObject(
                        name = "Basic Webhook",
                        summary = "Basic webhook registration",
                        value = OpenAPIExamples.WEBHOOK_REGISTRATION_REQ_EXAMPLE
                    ),
                    @ExampleObject(
                        name = "Without Description",
                        summary = "Webhook without description",
                        value = OpenAPIExamples.WEBHOOK_REGISTRATION_REQ_WITHOUT_DESCRIPTION
                    )
                }
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Webhook registered successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = WebhookDTO.Response.class),
                examples = @ExampleObject(value = OpenAPIExamples.WEBHOOK_RESPONSE_201_SUCCESS)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = OpenAPIExamples.WEBHOOK_ERROR_400_EXAMPLE)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = OpenAPIExamples.WEBHOOK_ERROR_500_EXAMPLE)
            )
        )
    })
    public ResponseEntity<WebhookDTO.Response> createWebhook(
            @Valid @RequestBody WebhookDTO.CreateRequest request) {
        
        WebhookDTO.Response response = webhookService.createWebhook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
        summary = "Get all webhooks",
        description = "Retrieve a list of all registered webhooks"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved webhooks",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = WebhookDTO.Response.class)),
                examples = @ExampleObject(value =OpenAPIExamples.WEBHOOK_200_EXAMPLES)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = OpenAPIExamples.WEBHOOK_ERROR_500_EXAMPLE)
            )
        )
    })
    public ResponseEntity<List<WebhookDTO.Response>> getAllWebhooks() {
        List<WebhookDTO.Response> webhooks = webhookService.getAllWebhooks();
        return ResponseEntity.ok(webhooks);
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete a webhook",
        description = "Delete a webhook by its ID"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Webhook deleted successfully"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Webhook not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = OpenAPIExamples.WEBHOOK_404_EXAMPLE)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = OpenAPIExamples.WEBHOOK_ERROR_500_EXAMPLE)
            )
        )
    })
    public ResponseEntity<Void> deleteWebhook(@PathVariable Long id) {
        webhookService.deleteWebhook(id);
        return ResponseEntity.noContent().build();
    }
}
