package com.demo.servicea.messaging.aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Gắn lên method mutation để aspect tự động diff before/after state
 * và publish DomainChangeEvent lên Kafka sau khi transaction commit.
 * <p>
 * Bắt buộc: method phải return aggregate root để aspect đọc after state
 * mà không cần flush thêm.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PublishDomainChanges {

    /**
     * Tên nghiệp vụ, dùng làm label trong event. Ví dụ: "product", "order"
     */
    String aggregateType();

    /**
     * Entity class tương ứng với aggregate, phải là @Entity JPA
     */
    Class<?> entityClass();

    /**
     * Index (0-based) của argument chứa JPA id trong method
     */
    int idParameterIndex() default 0;

    /**
     * Các property bỏ qua khi diff — mặc định bỏ version (optimistic lock)
     */
    String[] ignoredProperties() default {"version"};
}