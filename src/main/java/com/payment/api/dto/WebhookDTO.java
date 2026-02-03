package com.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

public class WebhookDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        
        @NotBlank(message = "URL is required")
        @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
        @Size(max = 500, message = "URL must not exceed 500 characters")
        private String url;

        @Size(max = 255, message = "Description must not exceed 255 characters")
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        
        private Long id;
        private String url;
        private String description;
        private Boolean active;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;
    }
}