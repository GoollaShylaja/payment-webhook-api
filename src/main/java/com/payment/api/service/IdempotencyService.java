package com.payment.api.service;

import com.payment.api.entity.IdempotencyRecord;
import com.payment.api.entity.IdempotencyRecord.IdempotencyStatus;
import com.payment.api.repository.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;

    @Value("${idempotency.expiry-hours:24}")
    private int expiryHours;

    /**
     * Looks up an existing completed record by key.
     * Returns the cached response body if found and not expired.
     */
    @Transactional(readOnly = true)
    public Optional<String> getCachedResponse(String idempotencyKey) {
        return idempotencyRepository.findByIdempotencyKey(idempotencyKey)
            .filter(r -> r.getStatus() == IdempotencyStatus.COMPLETED)
            .filter(r -> r.getExpiresAt().isAfter(LocalDateTime.now()))
            .map(IdempotencyRecord::getResponseBody);
    }

    /**
     * Tries to reserve the idempotency key (insert PROCESSING record).
     * Returns false if the key is already taken (concurrent or duplicate request).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryReserve(String idempotencyKey) {
        try {
            IdempotencyRecord record = new IdempotencyRecord(
                idempotencyKey,
                LocalDateTime.now().plusHours(expiryHours)
            );
            idempotencyRepository.saveAndFlush(record);
            return true;
        } catch (DataIntegrityViolationException e) {
            log.debug("Idempotency key already reserved: {}", idempotencyKey);
            return false;
        }
    }

    /**
     * Marks the key as COMPLETED and stores the response body.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(String idempotencyKey, String responseBody) {
        idempotencyRepository.findByIdempotencyKey(idempotencyKey).ifPresent(record -> {
            record.setResponseBody(responseBody);
            record.setStatus(IdempotencyStatus.COMPLETED);
            idempotencyRepository.save(record);
        });
    }

    /**
     * Removes PROCESSING record if payment processing failed,
     * so the client can retry with the same key.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(String idempotencyKey) {
        idempotencyRepository.findByIdempotencyKey(idempotencyKey)
            .filter(r -> r.getStatus() == IdempotencyStatus.PROCESSING)
            .ifPresent(idempotencyRepository::delete);
    }

    @Scheduled(cron = "${idempotency.cleanup-cron:0 0 * * * *}")
    @Transactional
    public void cleanupExpiredRecords() {
        log.debug("Cleaning up expired idempotency records");
        idempotencyRepository.deleteExpiredRecords(LocalDateTime.now());
    }
}
