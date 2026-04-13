package com.demo.servicea.messaging.aop;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.demo.servicea.entity.OutboxEvent;
import com.demo.servicea.messaging.DomainChangeDiff;
import com.demo.servicea.messaging.DomainChangeEvent;
import com.demo.servicea.messaging.EntitySnapshotReader;
import com.demo.servicea.messaging.EntityStateMapper;
import com.demo.servicea.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Intercept method có @PublishDomainChanges:
 * 1. Đọc before state
 * 2. Cho method thật chạy
 * 3. Đọc after state
 * 4. Diff → nếu có thay đổi thì INSERT vào outbox_events (cùng transaction)
 *
 * @Order(LOWEST_PRECEDENCE - 1): đảm bảo aspect bọc ngoài @Transactional,
 *                          nhờ đó afterCommit() mới hoạt động đúng.
 *                          <p>
 *                          Outbox thay thế việc gửi Kafka trực tiếp → không mất event khi app crash.
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
@RequiredArgsConstructor
public class DomainChangePublicationAspect {

    private final EntitySnapshotReader snapshotReader;
    private final EntityStateMapper entityStateMapper;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

        @Value("${app.kafka.product-updates-topic}")
        private String topic;

    @Around("@annotation(ann)")
    public Object aroundPublishableChange(ProceedingJoinPoint joinPoint, PublishDomainChanges ann) throws Throwable {
        Serializable id = extractId(joinPoint.getArgs(), ann);

        // Load before state — nếu không tìm thấy entity thì bỏ qua (create case)
        Object beforeEntity = snapshotReader.find(ann.entityClass(), id);
        Map<String, String> beforeState = beforeEntity == null
                ? Map.of()
                : entityStateMapper.readComparableState(beforeEntity, ann.ignoredProperties());

        Object result = joinPoint.proceed();

        if (beforeEntity == null)
            return result; // create — chưa handle, bỏ qua

        Map<String, String> afterState = resolveAfterState(ann, result, id);

        Optional<DomainChangeEvent> eventOpt = DomainChangeDiff.buildIfChanged(
                ann.aggregateType(), String.valueOf(id), beforeState, afterState
        );

        // Không có gì thay đổi → không cần insert outbox
        if (eventOpt.isEmpty())
            return result;

        saveToOutbox(eventOpt.get());

        return result;
    }

    /**
     * INSERT outbox record trong cùng transaction với business data.
     * Nếu transaction rollback → outbox cũng rollback → không bao giờ gửi event cho thay đổi bị huỷ.
     */
    private void saveToOutbox(DomainChangeEvent event) {
        try {
            OutboxEvent outbox = new OutboxEvent();
            outbox.setAggregateType(event.aggregateType());
            outbox.setAggregateId(event.aggregateId());
            outbox.setPayload(objectMapper.writeValueAsString(event));
            outbox.setTopic(topic);
            outbox.setCreatedAt(Instant.now());
            outboxRepository.save(outbox);
        } catch (Exception e) {
            // Throw để transaction rollback — không để business commit mà mất event
            throw new IllegalStateException("Failed to save outbox event", e);
        }
    }

    /**
     * Ưu tiên đọc after state từ return value (happy path, không cần flush).
     * Fallback sang flushAndReload chỉ khi method không trả về aggregate root.
     * Nếu không có transaction active → log warn và skip, không crash request.
     */
    private Map<String, String> resolveAfterState(PublishDomainChanges ann, Object result, Serializable id) {
        if (ann.entityClass().isInstance(result)) {
            return entityStateMapper.readComparableState(result, ann.ignoredProperties());
        }

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            Object reloaded = snapshotReader.flushAndReload(ann.entityClass(), id);
            if (reloaded == null)
                return Map.of();
            return entityStateMapper.readComparableState(reloaded, ann.ignoredProperties());
        }

        // Không crash — chỉ warn và skip event
        log.warn("@PublishDomainChanges: cannot read after state for {} id={} " +
                "— method should return the aggregate root. Event skipped.",
                ann.entityClass().getSimpleName(), id);
        return Map.of();
    }

    private static Serializable extractId(Object[] args, PublishDomainChanges ann) {
        int idx = ann.idParameterIndex();
        if (idx < 0 || idx >= args.length) {
            throw new IllegalStateException("idParameterIndex=" + idx + " out of range, args.length=" + args.length);
        }
        Object raw = args[idx];
        if (raw == null)
            throw new IllegalArgumentException("Aggregate id argument is null");
        if (raw instanceof Serializable s)
            return s;
        throw new IllegalArgumentException("Aggregate id must be Serializable, got: " + raw.getClass());
    }
}