package com.demo.servicea.messaging;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Tách JPA concern ra khỏi aspect.
 * Aspect không cần biết gì về EntityManager — chỉ gọi qua đây.
 */
@Component
public class EntitySnapshotReader {

    @PersistenceContext
    private EntityManager entityManager;

    public <T> T find(Class<T> clazz, Serializable id) {
        return entityManager.find(clazz, id);
    }

    /**
     * Flush + clear để reload entity sau khi transaction đã flush xuống DB.
     * Chỉ gọi khi transaction còn active.
     */
    public <T> T flushAndReload(Class<T> clazz, Serializable id) {
        entityManager.flush();
        entityManager.clear();
        return entityManager.find(clazz, id);
    }
}