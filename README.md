# Payment Webhook API

A Spring Boot application that provides secure payment processing with idempotency support, dynamic webhook notifications, circuit breaker resilience, and SSRF-protected outbound requests.

## Overview

- **Secure Payment Processing** — Card numbers encrypted with AES-256-GCM (random IV per encryption)
- **Idempotency** — Duplicate requests with the same `Idempotency-Key` return the original response without creating a second payment
- **Dynamic Webhook Registration** — Register endpoints to receive `PAYMENT_CREATED` notifications
- **Resilient Webhooks** — Automatic retry with exponential backoff and per-endpoint circuit breaker
- **SSRF Protection** — Webhook URLs are validated to block requests to private/internal addresses
- **RESTful API** — Proper HTTP status codes and input validation
- **Complete Documentation** — OpenAPI/Swagger specification

## Quick Start

### Prerequisites

- **Java 17+** — [Download](https://adoptium.net/)
- **Maven 3.6+** — [Download](https://maven.apache.org/download.cgi)
- **MySQL 8.0+** — [Download](https://dev.mysql.com/downloads/)

### Step 1: Setup Database

```sql
-- Connect: mysql -u root -p
CREATE DATABASE payment_api;
CREATE USER 'payment_user'@'localhost' IDENTIFIED BY 'payment_pass';
GRANT ALL PRIVILEGES ON payment_api.* TO 'payment_user'@'localhost';
FLUSH PRIVILEGES;
```

### Step 2: Clone and Build

```bash
git clone https://github.com/GoollaShylaja/payment-webhook-api
cd payment-webhook-api
mvn clean install
```

### Step 3: Run

```bash
mvn spring-boot:run
# Started PaymentWebhookApiApplication in X.XXX seconds
```

### Step 4: Test

Open **http://localhost:8080/swagger-ui.html** or use curl:

```bash
# 1. Register a webhook
curl -X POST http://localhost:8080/api/webhooks \
  -H "Content-Type: application/json" \
  -d '{"url": "https://webhook.site/your-unique-id", "description": "Test webhook"}'

# 2. Create a payment (idempotency key is optional but recommended)
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: a3f1c2e4-7b8d-4e5f-9a0b-1c2d3e4f5a6b" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "zipCode": "12345",
    "cardNumber": "4532015112830366"
  }'

# 3. Retry with the same key — returns original response, no duplicate charge
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: a3f1c2e4-7b8d-4e5f-9a0b-1c2d3e4f5a6b" \
  -d '{...same body...}'
```

**Response:**
```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "zipCode": "12345",
  "cardNumberMasked": "************0366",
  "createdAt": "2024-02-02T03:30:00"
}
```

## API Documentation

| URL | Description |
|-----|-------------|
| http://localhost:8080/swagger-ui.html | Swagger UI |
| http://localhost:8080/v3/api-docs | OpenAPI JSON |

## API Endpoints

### Payments

#### Create Payment
```
POST /api/payments
Content-Type: application/json
Idempotency-Key: <uuid>   (optional — recommended to prevent duplicate charges)
```

| Field | Type | Validation |
|-------|------|------------|
| `firstName` | String | Required, max 100 chars, letters/spaces/hyphens only |
| `lastName` | String | Required, max 100 chars, letters/spaces/hyphens only |
| `zipCode` | String | Required, format `12345` or `12345-6789` |
| `cardNumber` | String | Required, 13–19 digits |

**Response: 201 Created**

### Webhooks

#### Register Webhook
```
POST /api/webhooks
Content-Type: application/json

{
  "url": "https://your-endpoint.com/webhook",
  "description": "My webhook"
}
```

| Rule | Detail |
|------|--------|
| URL must be unique | Registering the same URL twice returns `400 Bad Request` |
| URL must be public | Private/internal IPs (`localhost`, `10.x.x.x`, `192.168.x.x`, etc.) are rejected |

**Response: 201 Created**

#### List All Webhooks
```
GET /api/webhooks
```
**Response: 200 OK**

#### Delete Webhook
```
DELETE /api/webhooks/{id}
```

| Response | Condition |
|----------|-----------|
| `204 No Content` | Webhook deleted successfully |
| `404 Not Found` | No webhook exists with that ID |

## Idempotency

Pass an `Idempotency-Key` header (UUID recommended) when creating payments to prevent duplicate charges from retries or double-clicks.

| Scenario | Behaviour |
|----------|-----------|
| First request with key | Payment created, response cached for 24 hours |
| Retry with same key | Cached response returned, no new payment created |
| No key provided | Payment created normally (no duplicate protection) |
| Failed payment + same key | Key released so client can retry safely |

Keys expire after 24 hours (configurable via `idempotency.expiry-hours`).

**Frontend pattern:**
```javascript
const idempotencyKey = localStorage.getItem('pending_payment_key') || crypto.randomUUID();
localStorage.setItem('pending_payment_key', idempotencyKey);

await fetch('/api/payments', {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey, 'Content-Type': 'application/json' },
    body: JSON.stringify(formData)
});

localStorage.removeItem('pending_payment_key'); // clear on success
```

## Webhook Notifications

When a payment is created, all active webhooks receive a `POST` with:

```json
{
  "eventType": "PAYMENT_CREATED",
  "timestamp": "2024-02-02T03:30:00",
  "payment": {
    "id": 1,
    "firstName": "John",
    "lastName": "Doe",
    "zipCode": "12345",
    "cardNumberMasked": "************0366",
    "createdAt": "2024-02-02T03:30:00"
  }
}
```

### Retry & Circuit Breaker

| Setting | Value |
|---------|-------|
| Max retry attempts | 3 |
| Backoff | Exponential — 5s, 10s, 20s |
| Circuit breaker failure threshold | 5 failures → OPEN |
| Circuit breaker recovery | 2 successes in HALF_OPEN → CLOSED |
| Wait before retry after OPEN | 60 seconds |

The circuit breaker is per webhook URL and resets on app restart.

## Security

| Feature | Detail |
|---------|--------|
| Card encryption | AES-256-GCM with a random 12-byte IV per encryption |
| Card masking | All but last 4 digits masked (e.g., `************0366`) |
| SSRF protection | Webhook hostnames resolved and checked against private IP ranges before sending |
| Duplicate webhooks | Unique constraint on webhook URL prevents duplicate notifications |
| Input validation | All fields validated with `javax.validation` constraints |
| Error responses | `ResourceNotFoundException` maps to precise 404; `IllegalArgumentException` maps to 400 |
| Secret key | Configure via environment variable in production (see Configuration) |

## Configuration

`src/main/resources/application.properties`:

```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/payment_api
spring.datasource.username=payment_user
spring.datasource.password=payment_pass

# Encryption — CHANGE IN PRODUCTION (use environment variable)
encryption.secret.key=MySecretKey12345MySecretKey12345

# Idempotency
idempotency.expiry-hours=24
idempotency.cleanup-cron=0 0 * * * *

# Webhook
webhook.retry.max-attempts=3
webhook.retry.initial-delay=5000
webhook.timeout.connect=5000
webhook.timeout.read=10000

# Circuit Breaker
circuit-breaker.failure-threshold=5
circuit-breaker.success-threshold=2
circuit-breaker.wait-duration-ms=60000
```

**Production encryption key via environment variable:**
```bash
export ENCRYPTION_SECRET_KEY=your-strong-random-key
```
```properties
encryption.secret.key=${ENCRYPTION_SECRET_KEY}
```

## Database Schema

Tables are auto-created by Hibernate (`ddl-auto=update`) on first run:

| Table | Description |
|-------|-------------|
| `payments` | Encrypted payment records |
| `webhooks` | Registered webhook endpoints |
| `idempotency_records` | Idempotency key cache (auto-cleaned hourly) |

## Project Structure

```
payment-webhook-api/
├── src/main/java/com/payment/api/
│   ├── circuitbreaker/      # Circuit breaker state machine
│   ├── config/              # Async executor, OpenAPI config
│   ├── controller/          # REST controllers + global exception handler
│   ├── dto/                 # Request/response DTOs
│   ├── entity/              # JPA entities (Payment, Webhook, IdempotencyRecord)
│   ├── exception/           # ResourceNotFoundException for precise 404 handling
│   ├── repository/          # Spring Data JPA repositories
│   ├── service/             # Business logic (Payment, Webhook, Idempotency)
│   └── util/                # EncryptionUtil (AES-256-GCM)
├── src/main/resources/
│   └── application.properties
├── src/test/                # Unit tests
├── pom.xml
└── README.md
```

## Technologies

| Technology | Version |
|------------|---------|
| Java | 17 |
| Spring Boot | 2.7.18 |
| Spring Data JPA | 2.7.18 |
| MySQL | 8.0 |
| Maven | 3.6+ |
| Lombok | Latest |
| SpringDoc OpenAPI | 3 |
| JUnit 5 + Mockito | Latest |

## Running Tests

```bash
# All tests
mvn test

# Specific class
mvn test -Dtest=PaymentServiceTest

# With coverage report
mvn clean test jacoco:report
# Report at: target/site/jacoco/index.html
```

## Troubleshooting

**Port already in use:**
```properties
server.port=8081
```

**Database connection failed:**
```bash
sudo systemctl status mysql  # Linux
brew services list            # Mac
```

**Webhooks not firing:**
```bash
# Check logs
grep "webhook" logs/application.log

# Verify active webhooks
SELECT * FROM webhooks WHERE active = true;
```

**Webhook rejected with "private/internal address" error:**
- Use a public URL (e.g., [webhook.site](https://webhook.site)) for testing
- `localhost` and private IP ranges are blocked by SSRF protection

**Registering a webhook returns 400 — URL already exists:**
- Each webhook URL must be unique — delete the existing one first or use a different URL:
```bash
curl -X DELETE http://localhost:8080/api/webhooks/{id}
```

**Idempotency key already in use / 500 error:**
- The key is still in `PROCESSING` state from a failed request
- Wait for the cleanup job (runs hourly) or delete the record manually:
```sql
DELETE FROM idempotency_records WHERE idempotency_key = 'your-key';
```

**Lombok errors in IDE:**
- IntelliJ: Install Lombok plugin + enable annotation processing
- VS Code: Install Java Extension Pack + Lombok extension
- Eclipse: Run `lombok.jar` installer

## License

MIT License
