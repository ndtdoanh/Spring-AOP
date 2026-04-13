package com.demo.serviceb.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "activity_logs", uniqueConstraints = {
        // Idempotency key: cùng topic+partition+offset không bao giờ insert 2 lần
        @UniqueConstraint(name = "uq_activity_log_kafka_position", columnNames = {"kafka_topic", "kafka_partition",
                "kafka_offset" })
})
@Getter
@Setter
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_type", nullable = false, length = 64)
    private String aggregateType;

    @Column(name = "aggregate_key", nullable = false, length = 128)
    private String aggregateKey;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "payload_json", nullable = false, columnDefinition = "text")
    private String payloadJson;

    // Kafka metadata — bộ 3 này là idempotency key
    @Column(name = "kafka_topic", nullable = false)
    private String kafkaTopic;

    @Column(name = "kafka_partition", nullable = false)
    private Integer kafkaPartition;

    @Column(name = "kafka_offset", nullable = false)
    private Long kafkaOffset;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}