package com.demo.serviceb.service;

import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.serviceb.entity.ActivityLog;
import com.demo.serviceb.ingest.InboundActivityEnvelope;
import com.demo.serviceb.ingest.model.DomainChangeEvent;
import com.demo.serviceb.repository.ActivityLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command service: nhận envelope từ Kafka listener → persist activity log.
 *
 * Idempotency: double-protection
 * 1. Check exists trước khi insert (fast path — tránh exception thông thường)
 * 2. Catch DataIntegrityViolationException (safety net — race condition giữa 2 consumer instance)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityLogCommandService {

    private final ActivityLogRepository activityLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void recordDomainChange(InboundActivityEnvelope envelope) {
        String topic = envelope.kafkaTopic();
        int partition = envelope.kafkaPartition();
        long offset = envelope.kafkaOffset();

        // Fast path idempotency check
        if (activityLogRepository.existsByKafkaTopicAndKafkaPartitionAndKafkaOffset(topic, partition, offset)) {
            log.warn("Duplicate event skipped: topic={} partition={} offset={}", topic, partition, offset);
            return;
        }

        try {
            ActivityLog entity = toEntity(envelope.event(), topic, partition, offset);
            activityLogRepository.save(entity);
        } catch (DataIntegrityViolationException e) {
            // Safety net: race condition giữa 2 pod cùng consume
            log.warn("Duplicate event on concurrent insert, skipped: topic={} partition={} offset={}", topic, partition,
                    offset);
        }
    }

    private ActivityLog toEntity(DomainChangeEvent event, String topic, int partition, long offset) {
        ActivityLog entity = new ActivityLog();
        entity.setAggregateType(event.aggregateType());
        entity.setAggregateKey(event.aggregateId());
        entity.setEventType("DOMAIN_CHANGE");
        entity.setPayloadJson(serialize(event));
        entity.setKafkaTopic(topic);
        entity.setKafkaPartition(partition);
        entity.setKafkaOffset(offset);
        entity.setCreatedAt(Instant.now());
        return entity;
    }

    private String serialize(DomainChangeEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize DomainChangeEvent", e);
        }
    }
}