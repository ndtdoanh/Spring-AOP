package com.demo.serviceb.ingest;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.demo.serviceb.ingest.model.DomainChangeEvent;
import com.demo.serviceb.service.ActivityLogCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Nhận raw String từ Kafka, tự deserialize sang DomainChangeEvent.
 *
 * Dùng String deserializer thay vì JsonDeserializer để:
 * - Không bị lock vào package name của producer
 * - Dễ debug — có thể log raw payload khi lỗi
 *
 * enable-auto-commit: false + ack-mode: RECORD
 * → chỉ commit offset sau khi persist DB thành công
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomainChangeListener {

    private final ActivityLogCommandService activityLogCommandService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.product-updates-topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onDomainChange(ConsumerRecord<String, String> record, Acknowledgment ack) {
        if (record.value() == null) {
            log.warn("Null payload at topic={} partition={} offset={} — skipping",
                    record.topic(), record.partition(), record.offset());
            ack.acknowledge();
            return;
        }

        try {
            DomainChangeEvent event = objectMapper.readValue(record.value(), DomainChangeEvent.class);
            activityLogCommandService.recordDomainChange(
                    new InboundActivityEnvelope(event, record.topic(), record.partition(), record.offset()));
            ack.acknowledge(); // commit offset sau khi persist thành công

        } catch (Exception e) {
            // Log raw payload để debug — không re-throw tránh poison pill block cả partition
            log.error("Failed to process record topic={} partition={} offset={} payload={}",
                    record.topic(), record.partition(), record.offset(), record.value(), e);
            ack.acknowledge(); // ack để không stuck — cân nhắc gửi DLQ nếu cần
        }
    }
}