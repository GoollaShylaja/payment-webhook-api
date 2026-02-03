package com.payment.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PaymentWebhookApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentWebhookApiApplication.class, args);
	}

}
