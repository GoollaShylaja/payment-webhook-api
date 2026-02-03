package com.payment.api.controller;

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
        description = "Register a webhook endpoint that will receive payment notifications"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Webhook registered successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = WebhookDTO.Response.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 1,
                        "url": "https://webhook.site/unique-id",
                        "description": "Primary notification endpoint",
                        "active": true,
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
                        "path": "/api/webhooks",
                        "details": ["url: URL must start with http:// or https://"]
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
                        "path": "/api/webhooks"
                    }
                """)
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
                examples = @ExampleObject(value = """
                    [
                        {
                            "id": 1,
                            "url": "https://webhook.site/abc-123",
                            "description": "Primary endpoint",
                            "active": true,
                            "createdAt": "2026-02-02T10:30:00"
                        },
                        {
                            "id": 2,
                            "url": "https://example.com/webhook",
                            "description": "Secondary endpoint",
                            "active": true,
                            "createdAt": "2026-02-02T11:00:00"
                        }
                    ]
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
                        "path": "/api/webhooks"
                    }
                """)
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
                examples = @ExampleObject(value = """
                    {
                        "timestamp": "2026-02-02T10:30:00",
                        "status": 404,
                        "error": "Not Found",
                        "message": "Webhook not found with ID: 999",
                        "path": "/api/webhooks/999"
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
                        "path": "/api/webhooks/1"
                    }
                """)
            )
        )
    })
    public ResponseEntity<Void> deleteWebhook(@PathVariable Long id) {
        webhookService.deleteWebhook(id);
        return ResponseEntity.noContent().build();
    }
}