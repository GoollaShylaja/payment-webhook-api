package com.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

public class PaymentDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "First name must contain only letters, spaces, hyphens, and apostrophes")
        private String firstName;

        @NotBlank(message = "Last name is required")
        @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "Last name must contain only letters, spaces, hyphens, and apostrophes")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        private String lastName;

        @NotBlank(message = "Zip code is required")
        @Pattern(regexp = "^[0-9]{5}(-[0-9]{4})?$", message = "Zip code must be 5 digits (e.g., 12345) or 5+4 format (e.g., 12345-6789)")
        private String zipCode;

        @NotBlank(message = "Card number is required")
        @Pattern(regexp = "^[0-9]{13,19}$", message = "Card number: digits only, length 13-19.")
        private String cardNumber;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        
        private Long id;
        private String firstName;
        private String lastName;
        private String zipCode;
        private String cardNumberMasked;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;
    }
}