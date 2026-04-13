package com.demo.servicea.messaging;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.stereotype.Component;

import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Đọc state của entity thành Map<String, String> để phục vụ diff.
 * <p>
 * Cache PropertyDescriptor per class — tránh reflection overhead mỗi request.
 */
@Component
public class EntityStateMapper {

    private static final Set<String> DEFAULT_SKIP = Set.of("class", "hibernateLazyInitializer", "handler");

    // Cache descriptor per entity class — computed once, reused mãi mãi
    private final Map<Class<?>, PropertyDescriptor[]> descriptorCache = new ConcurrentHashMap<>();

    public Map<String, String> readComparableState(Object entity, String[] ignoredFromAnnotation) {
        Set<String> ignored = new HashSet<>(DEFAULT_SKIP);
        ignored.addAll(Arrays.asList(ignoredFromAnnotation));

        PropertyDescriptor[] descriptors = descriptorCache.computeIfAbsent(
                entity.getClass(), BeanUtils::getPropertyDescriptors
        );

        BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(entity);
        Map<String, String> state = new LinkedHashMap<>();

        for (PropertyDescriptor pd : descriptors) {
            String name = pd.getName();
            if (ignored.contains(name) || pd.getReadMethod() == null) continue;
            state.put(name, stringify(wrapper.getPropertyValue(name)));
        }

        return state;
    }

    private static String stringify(Object value) {
        if (value == null) return null;
        if (value instanceof Enum<?> e) return e.name();
        return String.valueOf(value);
    }
}