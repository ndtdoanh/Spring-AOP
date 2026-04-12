package com.demo.servicea.messaging.aop;

import com.demo.servicea.messaging.DomainChangeDiff;
import com.demo.servicea.messaging.DomainChangeEvent;
import com.demo.servicea.messaging.DomainChangeKafkaNotifier;
import com.demo.servicea.messaging.EntityStateMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Aspect
@Component
@RequiredArgsConstructor
public class DomainChangePublicationAspect {

    @PersistenceContext
    private EntityManager entityManager;

    private final EntityStateMapper entityStateMapper;
    private final DomainChangeKafkaNotifier domainChangeKafkaNotifier;

    @Around("@annotation(com.demo.servicea.messaging.aop.PublishDomainChanges)")
    public Object aroundPublishableChange(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        PublishDomainChanges ann = method.getAnnotation(PublishDomainChanges.class);
        if (ann == null) {
            return joinPoint.proceed();
        }

        Object[] args = joinPoint.getArgs();
        if (ann.idParameterIndex() < 0 || ann.idParameterIndex() >= args.length) {
            throw new IllegalStateException("idParameterIndex out of range for " + method);
        }

        Serializable id = toSerializableId(args[ann.idParameterIndex()]);
        Object beforeEntity = entityManager.find(ann.entityClass(), id);
        Map<String, String> beforeState = beforeEntity == null
                ? Map.of()
                : entityStateMapper.readComparableState(beforeEntity, ann.ignoredProperties());

        Object result = joinPoint.proceed();

        if (beforeEntity == null) {
            return result;
        }

        /*
         * This aspect often runs *outside* TransactionInterceptor (lower @Order than default tx advisor).
         * After joinPoint.proceed() the transaction may already be committed, so EntityManager.flush()
         * throws TransactionRequiredException. Prefer reading the post-update state from the method return
         * value when it is the aggregate root; only use flush/reload when a tx is still active.
         */
        Map<String, String> afterState = resolveAfterState(ann, result, id);
        String aggregateKey = String.valueOf(id);

        Optional<DomainChangeEvent> event = DomainChangeDiff.buildIfChanged(
                ann.aggregateType(),
                aggregateKey,
                beforeState,
                afterState
        );

        if (event.isEmpty()) {
            return result;
        }

        DomainChangeEvent payload = event.get();
        Runnable send = () -> domainChangeKafkaNotifier.notify(payload);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }

        return result;
    }

    private Map<String, String> resolveAfterState(PublishDomainChanges ann, Object result, Serializable id) {
        if (ann.entityClass().isInstance(result)) {
            return entityStateMapper.readComparableState(result, ann.ignoredProperties());
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            entityManager.flush();
            entityManager.clear();
            Object afterEntity = entityManager.find(ann.entityClass(), id);
            if (afterEntity == null) {
                return Map.of();
            }
            return entityStateMapper.readComparableState(afterEntity, ann.ignoredProperties());
        }
        throw new IllegalStateException(
                "@PublishDomainChanges: after update, no active transaction — return the aggregate root ("
                        + ann.entityClass().getSimpleName()
                        + ") from the advised method, or lower this aspect's @Order so it runs inside @Transactional.");
    }

    private static Serializable toSerializableId(Object raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Aggregate id argument is null");
        }
        if (raw instanceof Serializable s) {
            return s;
        }
        throw new IllegalArgumentException("Aggregate id must be Serializable, got: " + raw.getClass());
    }
}
