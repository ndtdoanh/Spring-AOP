package com.demo.servicea.messaging.outbox;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.demo.servicea.entity.OutboxEvent;
import com.demo.servicea.repository.OutboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

/**
 * Poll outbox_events table và gửi lên Kafka.
 *
 * Fixes so với version cũ:
 * - Không filter theo aggregateType trong poller — đó là business logic, không thuộc đây
 * - kafkaTemplate.get() để block và biết chắc thành công trước khi delete
 * - @Transactional(readOnly = false) tường minh
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private static final int BATCH_SIZE = 50;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:5000}")
    @SchedulerLock(name = "OutboxPoller", lockAtMostFor = "PT5M", lockAtLeastFor = "PT30S")
    @Transactional
    public void process() {
        List<OutboxEvent> batch = outboxRepository.fetchBatch(BATCH_SIZE);
        if (batch.isEmpty())
            return;

        log.info("OutboxPoller processing {} events", batch.size());

        List<UUID> successIds = new ArrayList<>();

        for (OutboxEvent event : batch) {
            try {
                send(event);
                successIds.add(event.getId());
            } catch (Exception e) {
                // Fail per-record: log và tiếp tục, không dừng cả batch
                log.error("OutboxPoller failed to send event id={} aggregateType={} aggregateId={}",
                        event.getId(), event.getAggregateType(), event.getAggregateId(), e);
            }
        }

        if (!successIds.isEmpty()) {
            outboxRepository.deleteAllByIdInBatch(successIds);
            log.info("OutboxPoller deleted {} sent events", successIds.size());
        }
    }

    private void send(OutboxEvent event) throws Exception {
        ProducerRecord<String, String> record = new ProducerRecord<>(
                event.getTopic(),
                null,
                event.getAggregateId(), // partition key: đảm bảo cùng entity vào cùng partition
                event.getPayload());

        // Gắn metadata vào Kafka header để consumer đọc mà không cần deserialize payload
        record.headers()
                .add("messageId", event.getId().toString().getBytes(StandardCharsets.UTF_8))
                .add("aggregateType", event.getAggregateType().getBytes(StandardCharsets.UTF_8))
                .add("aggregateId", event.getAggregateId().getBytes(StandardCharsets.UTF_8))
                .add("occurredAt", event.getCreatedAt().toString().getBytes(StandardCharsets.UTF_8));

        // .get() block để biết chắc broker đã nhận trước khi delete
        // Trade-off: chậm hơn fire-and-forget nhưng đảm bảo at-least-once
        kafkaTemplate.send(record).get();
    }
}