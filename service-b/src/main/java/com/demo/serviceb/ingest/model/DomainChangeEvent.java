package com.demo.serviceb.ingest.model;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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