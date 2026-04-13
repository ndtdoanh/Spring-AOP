CREATE TABLE activity_logs (
                               id              BIGSERIAL PRIMARY KEY,
                               aggregate_type  VARCHAR(64)  NOT NULL,
                               aggregate_key   VARCHAR(128) NOT NULL,
                               event_type      VARCHAR(64)  NOT NULL,
                               payload_json    TEXT         NOT NULL,
                               kafka_topic     VARCHAR(256) NOT NULL,
                               kafka_partition INT          NOT NULL,
                               kafka_offset    BIGINT       NOT NULL,
                               created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- Idempotency key: ngăn insert trùng khi Kafka redelivery
                               CONSTRAINT uq_activity_log_kafka_position
                                   UNIQUE (kafka_topic, kafka_partition, kafka_offset)
);

CREATE INDEX idx_activity_logs_created_at ON activity_logs (created_at DESC);
CREATE INDEX idx_activity_logs_aggregate ON activity_logs (aggregate_type, aggregate_key);