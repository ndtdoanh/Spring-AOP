package com.demo.serviceb.ingest;

import com.demo.serviceb.ingest.model.DomainChangeEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DomainChangeListener {

    private final ActivityLogCommandService activityLogCommandService;

    @KafkaListener(
            topics = "${app.kafka.product-updates-topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onDomainChange(ConsumerRecord<String, DomainChangeEvent> record) {
        DomainChangeEvent payload = record.value();
        if (payload == null) {
            return;
        }
        activityLogCommandService.recordDomainChange(new InboundActivityEnvelope(payload, record));
    }
}
