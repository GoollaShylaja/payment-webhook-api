package com.payment.api.config;

public class OpenAPIExamples {

    public static final String PAYMENT_REQUEST_EXAMPLE = """
    {
        "firstName": "John",
        "lastName": "Doe",
        "zipCode": "12345",
        "cardNumber": "4532015112830366"
    }
    """;
    public static final String EXTENDED_ZIP_PAYMENT_REQUEST_EXAMPLE ="""
    {
        "firstName": "Jane",
        "lastName": "Smith",
        "zipCode": "12345-6789",
        "cardNumber": "5425233430109903"
    }
    """;

    public static final String PAYMENT_RESPONSE_SUCCESS ="""
    {
        "id": 1,
        "firstName": "John",
        "lastName": "Doe",
        "zipCode": "12345",
        "cardNumberMasked": "****0366",
        "createdAt": "2026-02-02T10:30:00"
    }
    """;
    public static final String PAYMENT_ERROR_400_EXAMPLE= """
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
    """;
    public static final String PAYMENT_ERROR_500_EXAMPLE="""
    {
        "timestamp": "2026-02-02T10:30:00",
        "status": 500,
        "error": "Internal Server Error",
        "message": "An unexpected error occurred",
        "path": "/api/payments"
    }
    """;
    public static final String WEBHOOK_REGISTRATION_REQ_EXAMPLE="""
    {
        "url": "https://webhook.site/unique-id",
        "description": "Primary notification endpoint"
    }
    """;
    public static final String WEBHOOK_REGISTRATION_REQ_WITHOUT_DESCRIPTION="""
    {
        "url": "https://example.com/webhook"
    }
    """;
    public static final String WEBHOOK_RESPONSE_201_SUCCESS="""
    {
        "id": 1,
        "url": "https://webhook.site/unique-id",
        "description": "Primary notification endpoint",
        "active": true,
        "createdAt": "2026-02-02T10:30:00"
    }
    """;
    public static final String WEBHOOK_ERROR_400_EXAMPLE="""
    {
        "timestamp": "2026-02-02T10:30:00",
        "status": 400,
        "error": "Validation Failed",
        "message": "Invalid input data",
        "path": "/api/webhooks",
        "details": ["url: URL must start with http:// or https://"]
    }
    """;
    public static final String WEBHOOK_ERROR_500_EXAMPLE="""
    {
        "timestamp": "2026-02-02T10:30:00",
        "status": 500,
        "error": "Internal Server Error",
        "message": "An unexpected error occurred",
        "path": "/api/webhooks"
    }
    """;
    public static final String WEBHOOK_200_EXAMPLES="""
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
    """;
    public static final String WEBHOOK_404_EXAMPLE="""
    {
        "timestamp": "2026-02-02T10:30:00",
        "status": 404,
        "error": "Not Found",
        "message": "Webhook not found with ID: 999",
        "path": "/api/webhooks/999"
    }
    """;
}
