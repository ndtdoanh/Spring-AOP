package com.demo.servicea.repository;

import com.demo.servicea.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;


public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Lấy batch theo thứ tự created_at để đảm bảo gửi đúng thứ tự.
     * LIMIT trực tiếp trong query — không dùng Pageable để tránh count query thừa.
     */
    @Query(value = "SELECT * FROM outbox_events ORDER BY created_at ASC LIMIT :limit", nativeQuery = true)
    List<OutboxEvent> fetchBatch(@Param("limit") int limit);
}
