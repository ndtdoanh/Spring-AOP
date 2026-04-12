package com.demo.serviceb.ingest.aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Cross-cutting persistence hook for activity ingestion: metadata enrichment, JSON
 * materialisation, and transactional boundaries stay out of Kafka listeners.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PersistActivityLog {
}
