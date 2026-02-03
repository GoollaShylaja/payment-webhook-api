# Payment Webhook API

A Spring Boot application that provides secure payment processing with dynamic webhook notifications and resilient retry mechanisms.

## Overview

This application provides:

    Secure Payment Processing** - Card numbers encrypted with AES-256
    Dynamic Webhook Registration** - Register endpoints to receive payment notifications
    Resilient Webhooks** - Automatic retry with exponential backoff (up to 3 attempts)
    RESTful API** - Proper HTTP status codes and validation
    Complete Documentation** - OpenAPI/Swagger specification

## Quick Start (5 Minutes)

### Prerequisites

- **Java 17 or higher** ([Download here](https://adoptium.net/))
- **Maven 3.6+** ([Download here](https://maven.apache.org/download.cgi))
- **MySQL 8.0+** ([Download here](https://dev.mysql.com/downloads/))

### Step 1: Verify Prerequisites

      # Check Java (must be 17+)
      java -version

      # Check Maven
      mvn -version

      # Check MySQL is running
      mysql --version

  ### Step 2: Setup Database

    # Connect to MySQL
    mysql -u root -p

    # Run these commands:
    CREATE DATABASE payment_api;
    CREATE USER 'payment_user'@'localhost' IDENTIFIED BY 'payment_pass';
    GRANT ALL PRIVILEGES ON payment_api.* TO 'payment_user'@'localhost';
    FLUSH PRIVILEGES;
    EXIT;

    Or use the provided script: mysql -u root -p < database-setup.sql

### Step 3: Clone and Build

  # Clone the repository
  git clone https://github.com/GoollaShylaja/payment-webhook-api
  cd payment-webhook-api

  # Build the project
  mvn clean install

### **Step 4: Run the Application
  
  mvn spring-boot:run

  **You should see:**
  Started PaymentWebhookApiApplication in X.XXX seconds

### Step 5: Test It!

Open your browser: **http://localhost:8080/swagger-ui.html**

Or test with curl:


# 1. Register a webhook (use webhook.site for testing)
curl -X POST http://localhost:8080/api/webhooks \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://webhook.site/your-unique-id",
    "description": "Test webhook"
  }'

# 2. Create a payment
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "zipCode": "12345",
    "cardNumber": "4532015112830366"
  }'

# 3. Check webhook.site to see the notification!

**Response Example:**
json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "zipCode": "12345",
  "cardNumberMasked": "****0366",
  "createdAt": "2024-02-02T3:30:00"
}


## API Documentation

Once running, access documentation at:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **OpenAPI YAML**: See `openapi.yaml` in project root

## API Endpoints

### Payments

#### Test the API

Option A: Using cURL

    1.Register a webhook (use webhook.site for testing):
        curl -X POST http://localhost:8080/api/webhooks \
        -H "Content-Type: application/json" \
        -d '{
            "url": "https://webhook.site/unique-id",
            "description": "Test webhook"
        }'    

    2.Create a payment:
        curl -X POST http://localhost:8080/api/payments \
        -H "Content-Type: application/json" \
        -d '{
            "firstName": "John",
            "lastName": "Doe",
            "zipCode": "12345",
            "cardNumber": "4532015112830366"
        }'
    3.Check your webhook at webhook.site to see the notification!

Option B: Using Postman

    1.Import postman-collection.json
    2.Update webhook URL in "Register Webhook" request
    3.Run requests in order

Option C: Using Swagger UI

    Open browser: http://localhost:8080/swagger-ui.html

    Verify Everything Works
    ✅ Payment created (returns 201 status)
    ✅ Card number is masked (shows ****0366)
    ✅ Webhook received notification
    ✅ Swagger UI loads successfully


### Webhooks

#### Register Webhook
http
POST /api/webhooks
Content-Type: application/json

{
  "url": "https://your-endpoint.com/webhook",
  "description": "My webhook endpoint"
}

**Response: 201 Created**
json
{
  "id": 1,
  "url": "https://your-endpoint.com/webhook",
  "description": "My webhook endpoint",
  "active": true,
  "createdAt": "2024-02-02T3:30:00"
}

#### List All Webhooks
http
GET /api/webhooks

**Response: 200 OK**
json
[
  {
    "id": 1,
    "url": "https://your-endpoint.com/webhook",
    "description": "My webhook endpoint",
    "active": true,
    "createdAt": "2024-02-02T3:30:00"
  }
]

#### Delete Webhook
http
DELETE /api/webhooks/{id}

**Response: 204 No Content**

## Webhook Notifications

When a payment is created, all active webhooks receive:

json
{
  "eventType": "PAYMENT_CREATED",
  "timestamp": "2024-02-02T3:30:00",
  "payment": {
    "id": 1,
    "firstName": "John",
    "lastName": "Doe",
    "zipCode": "12345",
    "cardNumberMasked": "****0366",
    "createdAt": "2024-02-02T3:30:00"
  }
}

### Webhook Retry Mechanism

- **Attempts**: Up to 3 retries
- **Backoff**: Exponential (5s, 10s, 20s)
- **Async**: Non-blocking payment creation
- **Logging**: Comprehensive error tracking

## Security Features

- **AES-256 Encryption** for card numbers
- **Masked Display** - Only last 4 digits shown in responses
- **Input Validation** - All endpoints validated
- **Environment Variables** - Secure secret management


## Technologies Used

- **Java 17**
- **Spring Boot 2.7.18**
- **Spring Data JPA**
- **MySQL 8.0**
- **Maven**
- **Lombok**
- **SpringDoc OpenAPI 3**
- **JUnit 5 & Mockito**

## Project Structure

payment-webhook-api/
├── src/
│   ├── main/
│   │   ├── java/com/payment/api/
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── dto/             # Data transfer objects
│   │   │   ├── entity/          # JPA entities
│   │   │   ├── repository/      # Data repositories
│   │   │   ├── service/         # Business logic
│   │   │   └── util/            # Utilities (encryption)
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       ├── java/                # Unit & integration tests
│       └── resources/
├── openapi.yaml                 # OpenAPI specification
├── pom.xml                      # Maven configuration
└── README.md                    # This file

## 🧪 Running Tests

# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=PaymentServiceTest

# Run with coverage
mvn clean test jacoco:report
# View report at: target/site/jacoco/index.html

## ⚙️ Configuration

### Database Configuration

Update `src/main/resources/application.properties`:

properties
spring.datasource.url=jdbc:mysql://localhost:3306/payment_api
spring.datasource.username=payment_user
spring.datasource.password=payment_pass


### Application Port

properties
server.port=8080


### Encryption Key

 **IMPORTANT**: Change this for production!

properties
encryption.secret.key=MySecretKey12345MySecretKey12345


### Webhook Configuration

properties
webhook.retry.max-attempts=3
webhook.retry.initial-delay=5000
webhook.timeout.connect=5000
webhook.timeout.read=10000


## Troubleshooting

### Application Won't Start

**Issue**: Port already in use

# Change port in application.properties
server.port=8081


**Issue**: Database connection failed

# Verify MySQL is running
sudo systemctl status mysql  # Linux
brew services list  # Mac

# Check credentials in application.properties

### Build Errors

**Issue**: `cannot find symbol: method setFirstName`

# Lombok not configured
# See LOMBOK-SETUP.md for detailed instructions

# Quick fix for IntelliJ:
# 1. Install Lombok plugin
# 2. Enable annotation processing
# 3. Reload Maven project

**Issue**: `maven-compiler-plugin error`

# See EXACT-FIX.md for the solution
# TL;DR: Use Java 17 and maven-compiler-plugin 3.11.0


### Webhook Issues

**Issue**: Webhooks not firing

# Check application logs
grep "webhook" logs/application.log

# Verify webhooks are active
SELECT * FROM webhooks WHERE active = true;

# Test endpoint manually
curl -X POST https://your-webhook-url \
  -H "Content-Type: application/json" \
  -d '{"test": "data"}'


## Additional Documentation

- **EXACT-FIX.md** - Troubleshooting compilation errors
- **LOMBOK-SETUP.md** - Lombok installation and configuration
- **VSCODE-SETUP-MAC.md** - VS Code setup for Mac
- **TESTING.md** - Comprehensive testing guide
- **PROJECT-SUMMARY.md** - Complete project overview

## 🎯 Example Usage Workflow

# 1. Start the application
mvn spring-boot:run

# 2. Register a webhook (get a unique URL from webhook.site)
curl -X POST http://localhost:8080/api/webhooks \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://webhook.site/abc-123",
    "description": "Test webhook"
  }'

# 3. Create a payment
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Alice",
    "lastName": "Smith",
    "zipCode": "90210",
    "cardNumber": "5425233430109903"
  }'

# 4. Check webhook.site - you'll see the notification!

# 5. List all webhooks
curl http://localhost:8080/api/webhooks

# 6. Delete a webhook
curl -X DELETE http://localhost:8080/api/webhooks/1


## Development

### IDE Setup

**IntelliJ IDEA:**
1. Install Lombok plugin
2. Enable annotation processing
3. Import as Maven project

**VS Code:**
1. Install Java Extension Pack
2. Install Lombok extension
3. See VSCODE-SETUP-MAC.md

**Eclipse:**
1. Install Lombok (run lombok.jar)
2. Import as Maven project

### Hot Reload (Development)

Spring Boot DevTools is already included:


mvn spring-boot:run

# Edit code and save - application auto-restarts!


### Database Schema

Tables are auto-created by Hibernate on first run:

- **payments** - Stores encrypted payment information
- **webhooks** - Stores registered webhook endpoints

## 📝 License

MIT License

For issues or questions:

1. Check the troubleshooting section above
2. Review additional documentation files
3. Check application logs for detailed errors
4. Verify all prerequisites are installed correctly

Future Improvements
Idempotency (Design Consideration)

Each payment is treated as an independent transaction. For example, using the same card on different days will create separate payment records, which is expected behavior in payment systems.

However, duplicate API requests sent within a short time window (due to user double-clicks, network retries, or client-side issues) can result in unintended duplicate charges. Preventing this typically requires idempotency.

Idempotency is an industry-standard approach used by payment providers such as Stripe and PayPal, where repeated requests with the same idempotency key result in only one payment being created. While idempotency is not implemented in this version of the application, the system is designed to support it as a future enhancement.