Testing & Deployment Guide
Running Tests

Run All Tests
    mvn test

Run Specific Test Class
    mvn test -Dtest=PaymentServiceTest

Run with Coverage Report
    mvn clean test jacoco:report
# View report at: target/site/jacoco/index.html

Manual Testing Scenarios

Scenario 1: Complete Payment Flow

    i.Start the application
        mvn spring-boot:run

    ii.Register a test webhook
        curl -X POST http://localhost:8080/api/webhooks \
        -H "Content-Type: application/json" \
        -d '{
            "url": "https://webhook.site/unique-id",
            "description": "Primary webhook"
        }'
    
        Expected: 201 Created with webhook ID

    iii.Create a payment

        curl -X POST http://localhost:8080/api/payments \
        -H "Content-Type: application/json" \
        -d '{
            "firstName": "Alice",
            "lastName": "Johnson",
            "zipCode": "90210",
            "cardNumber": "4532015112830366"
        }'
        Expected:
        201 Created
        Returns masked card: ****0366
        Webhook receives notification asynchronously

    iv.Verify webhook notification
        Check webhook.site
        Should see JSON payload with payment details
        
Scenario 2: Validation Testing

    i. Test invalid card number:
        curl -X POST http://localhost:8080/api/payments \
        -H "Content-Type: application/json" \
        -d '{
            "firstName": "Bob",
            "lastName": "Smith",
            "zipCode": "12345",
            "cardNumber": "123"
        }'

        Expected: 400 Bad Request with validation error

    ii.Test invalid zip code:
        curl -X POST http://localhost:8080/api/payments \
        -H "Content-Type: application/json" \
        -d '{
            "firstName": "Carol",
            "lastName": "White",
            "zipCode": "ABCDE",
            "cardNumber": "4532015112830366"
        }'

        Expected: 400 Bad Request

    iii.Test missing fields:

        curl -X POST http://localhost:8080/api/payments \
        -H "Content-Type: application/json" \
        -d '{
            "firstName": "Dave"
        }'

        Expected: 400 Bad Request with multiple validation errors

Scenario 3: Webhook Retry Testing

    i. Register an invalid webhook (to test retry mechanism):
        curl -X POST http://localhost:8080/api/webhooks \
        -H "Content-Type: application/json" \
        -d '{
            "url": "http://invalid-webhook-url-12345.com/webhook",
            "description": "Invalid webhook for testing retries"
        }'

    ii.Create a payment
    iii.Check logs - you should see retry attempts with exponential backoff

Scenario 4: Multiple Webhooks

    i.Register multiple webhooks:

        # Webhook 1

            curl -X POST http://localhost:8080/api/webhooks \
            -H "Content-Type: application/json" \
            -d '{"url": "https://webhook.site/id-1", "description": "Webhook 1"}'

        # Webhook 2

            curl -X POST http://localhost:8080/api/webhooks \
            -H "Content-Type: application/json" \
            -d '{"url": "https://webhook.site/id-2", "description": "Webhook 2"}'

    ii.List all webhooks:

        curl http://localhost:8080/api/webhooks

    iii.Create a payment - both webhooks should receive notifications

    iv.Delete a webhook:
        curl -X DELETE http://localhost:8080/api/webhooks/1

Performance Testing

    Load Testing with Apache Bench
    # Install Apache Bench
    sudo apt-get install apache2-utils  # Ubuntu/Debian
    brew install httpd  # macOS

    # Test payment creation (adjust -n and -c as needed)
    ab -n 100 -c 10 -p payment.json -T application/json \
    http://localhost:8080/api/payments

    Create payment.json:
    {
    "firstName": "Load",
    "lastName": "Test",
    "zipCode": "12345",
    "cardNumber": "4532015112830366"
    }

Security Testing

Test Card Encryption

1.Create a payment

    i.Check database directly:
    ii.USE payment_api;

        SELECT id, first_name, last_name, card_number_encrypted, card_number_masked 
        FROM payments ORDER BY id DESC LIMIT 1;

    ii.Verify:
        card_number_encrypted is encrypted (not plain text)
        card_number_masked shows only last 4 digits

Test API Input Validation

    Try various malicious inputs:

        # SQL Injection attempt
        curl -X POST http://localhost:8080/api/payments \
        -H "Content-Type: application/json" \
        -d '{
            "firstName": "Robert'; DROP TABLE payments;--",
            "lastName": "Doe",
            "zipCode": "12345",
            "cardNumber": "4532015112830366"
        }'

        Should be safely handled by JPA/Hibernate.

Deployment

Package for Production
    # Create JAR file
    mvn clean package -DskipTests

# JAR location
    ls -lh target/payment-webhook-api-1.0.0.jar

Environment Configuration

Create application-prod.properties:
    spring.datasource.url=jdbc:mysql://prod-db-host:3306/payment_api
    spring.datasource.username=${DB_USERNAME}
    spring.datasource.password=${DB_PASSWORD}
    encryption.secret.key=${ENCRYPTION_KEY}

Run in Production
# Set environment variables
export DB_USERNAME=prod_user
export DB_PASSWORD=secure_password
export ENCRYPTION_KEY=production-key-min-32-chars

# Run with production profile
java -jar target/payment-webhook-api-1.0.0.jar \
  --spring.profiles.active=prod

Log Monitoring
# Watch logs in real-time
tail -f logs/application.log

# Search for errors
grep ERROR logs/application.log

# Watch webhook notifications
grep "webhook" logs/application.log

Database Monitoring

Check payment count
    SELECT COUNT(*) FROM payments;

Check recent payments
    SELECT id, first_name, last_name, card_number_masked, created_at 
    FROM payments 
    ORDER BY created_at DESC 
    LIMIT 10;

Check active webhooks
    SELECT id, url, description, active 
    FROM webhooks 
    WHERE active = true;

Monitor payment creation rate
    SELECT DATE(created_at) as date, COUNT(*) as count 
    FROM payments 
    GROUP BY DATE(created_at) 
    ORDER BY date DESC;

Troubleshooting
Application won't start
Check Java version: java -version
Check if port 8080 is in use: lsof -i :8080
Review application.properties
Check database connectivity
Webhooks not firing
Check webhook URL is accessible
Review application logs
Verify webhooks are active: SELECT * FROM webhooks WHERE active = true;
Test webhook endpoint manually with curl
Database errors
Verify MySQL is running
Check database exists: SHOW DATABASES;
Verify user permissions
Check connection string in application.properties
Best Practices
Always run tests before deployment
Use environment variables for sensitive data
Monitor application logs regularly
Set up database backups
Use HTTPS in production
Implement rate limiting for production
Set up proper logging and monitoring
Regular security audits
CI/CD Integration
GitHub Actions Example
Create .github/workflows/ci.yml:
name: CI

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v2
    - name: Set up JDK 11
      uses: actions/setup-java@v2
      with:
        java-version: '11'
    - name: Build with Maven
      run: mvn clean install
    - name: Run tests
      run: mvn test

