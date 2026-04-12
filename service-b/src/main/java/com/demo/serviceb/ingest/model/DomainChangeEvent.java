package com.demo.serviceb.ingest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DomainChangeEvent(
        String aggregateType,
        String aggregateId,
        Map<String, FieldChange> changes,
        Instant occurredAt
) {
    public record FieldChange(String previous, String current) {
    }
}
