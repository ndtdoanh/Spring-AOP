ALTER TABLE activity_logs ADD COLUMN aggregate_type VARCHAR(64);
ALTER TABLE activity_logs ADD COLUMN aggregate_key VARCHAR(128);

UPDATE activity_logs
SET aggregate_type = 'product',
    aggregate_key = CAST(product_id AS VARCHAR(32))
WHERE aggregate_type IS NULL;

ALTER TABLE activity_logs ALTER COLUMN aggregate_type SET NOT NULL;
ALTER TABLE activity_logs ALTER COLUMN aggregate_key SET NOT NULL;

ALTER TABLE activity_logs DROP COLUMN product_id;

DROP INDEX IF EXISTS idx_activity_logs_product_id;

CREATE INDEX idx_activity_logs_aggregate ON activity_logs (aggregate_type, aggregate_key);
