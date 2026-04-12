package com.demo.serviceb.ingest;

import com.demo.serviceb.ingest.model.DomainChangeEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public record InboundActivityEnvelope(
        DomainChangeEvent event,
        ConsumerRecord<String, DomainChangeEvent> record
) {
}
