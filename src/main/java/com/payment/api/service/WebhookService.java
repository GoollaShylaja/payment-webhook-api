package com.payment.api.service;

import com.payment.api.dto.WebhookDTO;
import com.payment.api.entity.Webhook;
import com.payment.api.repository.WebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final WebhookRepository webhookRepository;

    @Transactional
    public WebhookDTO.Response createWebhook(WebhookDTO.CreateRequest request) {
        log.info("Creating webhook for URL: {}", request.getUrl());

        Webhook webhook = new Webhook();
        webhook.setUrl(request.getUrl());
        webhook.setDescription(request.getDescription());
        webhook.setActive(true);

        Webhook savedWebhook = webhookRepository.save(webhook);
        log.info("Webhook created with ID: {}", savedWebhook.getId());

        return toResponseDTO(savedWebhook);
    }

    public List<WebhookDTO.Response> getAllWebhooks() {
        return webhookRepository.findAll().stream()
            .map(this::toResponseDTO)
            .collect(Collectors.toList());
    }

    public List<Webhook> getActiveWebhooks() {
        return webhookRepository.findByActiveTrue();
    }

    @Transactional
    public void deleteWebhook(Long id) {
        log.info("Deleting webhook with ID: {}", id);
        
        if (!webhookRepository.existsById(id)) {
            throw new IllegalArgumentException("Webhook not found with ID: " + id);
        }
        
        webhookRepository.deleteById(id);
        log.info("Webhook deleted successfully");
    }

    private WebhookDTO.Response toResponseDTO(Webhook webhook) {
        return new WebhookDTO.Response(
            webhook.getId(),
            webhook.getUrl(),
            webhook.getDescription(),
            webhook.getActive(),
            webhook.getCreatedAt()
        );
    }
}