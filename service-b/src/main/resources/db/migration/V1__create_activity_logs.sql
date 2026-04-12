CREATE TABLE activity_logs (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload_json TEXT NOT NULL,
    kafka_topic VARCHAR(255),
    kafka_partition INT,
    kafka_offset BIGINT,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_activity_logs_product_id ON activity_logs (product_id);
