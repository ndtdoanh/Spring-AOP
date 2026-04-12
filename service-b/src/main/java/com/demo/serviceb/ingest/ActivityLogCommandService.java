package com.demo.serviceb.ingest;

import com.demo.serviceb.entity.ActivityLog;
import com.demo.serviceb.ingest.aop.PersistActivityLog;
import com.demo.serviceb.ingest.model.DomainChangeEvent;
import com.demo.serviceb.repository.ActivityLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityLogCommandService {

    private final ActivityLogRepository activityLogRepository;
    private final ObjectMapper objectMapper;

    @PersistActivityLog
    @Transactional
    public void recordDomainChange(InboundActivityEnvelope envelope) {
        DomainChangeEvent event = envelope.event();
        ActivityLog entity = new ActivityLog();
        entity.setAggregateType(event.aggregateType());
        entity.setAggregateKey(event.aggregateId());
        entity.setEventType("DOMAIN_CHANGE");
        entity.setPayloadJson(writePayload(event));
        if (envelope.record() != null) {
            entity.setKafkaTopic(envelope.record().topic());
            entity.setKafkaPartition(envelope.record().partition());
            entity.setKafkaOffset(envelope.record().offset());
        }
        entity.setCreatedAt(Instant.now());
        activityLogRepository.save(entity);
    }

    private String writePayload(DomainChangeEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialise inbound event", ex);
        }
    }
}
