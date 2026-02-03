-- Database Setup Script for Payment Webhook API
-- Execute this script to set up the database

-- Create database
CREATE DATABASE IF NOT EXISTS payment_api;

-- Create user (change password in production!)
CREATE USER IF NOT EXISTS 'payment_user'@'localhost' IDENTIFIED BY 'payment_pass';

-- Grant privileges
GRANT ALL PRIVILEGES ON payment_api.* TO 'payment_user'@'localhost';

-- Apply privileges
FLUSH PRIVILEGES;

-- Use the database
USE payment_api;

-- Tables will be created automatically by Hibernate
-- But here's the schema for reference:

/*
CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    zip_code VARCHAR(20) NOT NULL,
    card_number_encrypted VARCHAR(500) NOT NULL,
    card_number_masked VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS webhooks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    url VARCHAR(500) NOT NULL,
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
*/