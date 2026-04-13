package com.demo.serviceb.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.demo.serviceb.entity.ActivityLog;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    /** Dùng để check idempotency trước khi insert */
    boolean existsByKafkaTopicAndKafkaPartitionAndKafkaOffset(
            String topic, int partition, long offset);
}
