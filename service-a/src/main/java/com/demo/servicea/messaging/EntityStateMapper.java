package com.demo.servicea.messaging;

import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.stereotype.Component;

@Component
public class EntityStateMapper {

    private static final Set<String> DEFAULT_SKIP = Set.of("class", "hibernateLazyInitializer", "handler");

    public Map<String, String> readComparableState(Object entity, String[] ignoredFromAnnotation) {
        Set<String> ignored = new LinkedHashSet<>(DEFAULT_SKIP);
        ignored.addAll(Arrays.asList(ignoredFromAnnotation));

        BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(entity);
        Map<String, String> state = new LinkedHashMap<>();
        for (PropertyDescriptor pd : BeanUtils.getPropertyDescriptors(entity.getClass())) {
            String name = pd.getName();
            if (ignored.contains(name) || pd.getReadMethod() == null) {
                continue;
            }
            Object value = wrapper.getPropertyValue(name);
            state.put(name, stringify(value));
        }
        return state;
    }

    private static String stringify(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Enum<?> e) {
            return e.name();
        }
        return String.valueOf(value);
    }
}
