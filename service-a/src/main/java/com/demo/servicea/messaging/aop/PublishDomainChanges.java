package com.demo.servicea.messaging.aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a transactional method that mutates a JPA aggregate. After success, the aspect diffs selected
 * JavaBean properties and publishes a generic {@link com.demo.servicea.messaging.DomainChangeEvent}.
 * <p>
 * Prefer returning the aggregate root from the advised method so the aspect can read the new state
 * without calling {@code EntityManager.flush()} (which fails when this advice runs outside the
 * transaction boundary).
 * <p>
 * Add this annotation per mutating method with metadata — no extra snapshot class per entity.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PublishDomainChanges {

    /** Stable name for consumers, e.g. {@code "product"}, {@code "order"}. */
    String aggregateType();

    /** Root entity class; must match the {@link jakarta.persistence.Entity} loaded by primary key. */
    Class<?> entityClass();

    /** Zero-based index of the method argument that holds the JPA identifier ({@link Long}, {@link String}, …). */
    int idParameterIndex() default 0;

    /**
     * JavaBean property names excluded from diff (optimistic lock, relations you do not want in events, …).
     */
    String[] ignoredProperties() default {"version"};
}
