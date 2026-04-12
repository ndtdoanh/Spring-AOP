package com.demo.servicea.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DomainChangeKafkaNotifier {

    private final KafkaTemplate<String, DomainChangeEvent> kafkaTemplate;

    @Value("${app.kafka.product-updates-topic}")
    private String topic;

    public void notify(DomainChangeEvent event) {
        String key = event.aggregateType() + ":" + event.aggregateId();
        kafkaTemplate.send(topic, key, event);
    }
}
