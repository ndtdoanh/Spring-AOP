package com.demo.servicea.messaging;

import java.time.Instant;
import java.util.Map;

/**
 * Payload được lưu vào outbox và cuối cùng gửi lên Kafka
 */
public record DomainChangeEvent(
        String aggregateType,
        String aggregateId,
        Map<String, FieldChange> changes,
        Instant occurredAt
) {
    public record FieldChange(String previous, String current) {
    }
}