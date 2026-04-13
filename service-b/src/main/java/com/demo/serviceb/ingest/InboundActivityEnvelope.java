package com.demo.serviceb.ingest;

import com.demo.serviceb.ingest.model.DomainChangeEvent;

/**
 * Bọc event và Kafka metadata lại với nhau để truyền xuống command service.
 * Tách metadata ra field riêng — không phụ thuộc vào generic type của ConsumerRecord.
 */
public record InboundActivityEnvelope(
        DomainChangeEvent event,
        String kafkaTopic,
        int kafkaPartition,
        long kafkaOffset) {
}