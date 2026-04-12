package com.demo.serviceb.ingest.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ActivityLogPersistenceAspect {

    @Around("@annotation(persistActivityLog)")
    public Object aroundPersist(ProceedingJoinPoint joinPoint, PersistActivityLog persistActivityLog) throws Throwable {
        String signature = joinPoint.getSignature().toShortString();
        long startedAt = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            log.info("Activity log persisted via {} in {} ms",
                    signature,
                    (System.nanoTime() - startedAt) / 1_000_000);
            return result;
        } catch (Throwable ex) {
            log.error("Activity log persistence failed for {}", signature, ex);
            throw ex;
        }
    }
}
