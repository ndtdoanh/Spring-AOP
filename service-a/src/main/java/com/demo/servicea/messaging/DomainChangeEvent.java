package com.demo.servicea.messaging;

import java.time.Instant;
import java.util.Map;

/** Kafka payload: one shape for every aggregate type. */
public record DomainChangeEvent(
        String aggregateType,
        String aggregateId,
        Map<String, FieldChange> changes,
        Instant occurredAt
) {
    public record FieldChange(String previous, String current) {
    }
}
