package com.demo.servicea.messaging;

import com.demo.servicea.messaging.DomainChangeEvent.FieldChange;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class DomainChangeDiff {

    private DomainChangeDiff() {
    }

    public static Optional<DomainChangeEvent> buildIfChanged(
            String aggregateType,
            String aggregateId,
            Map<String, String> before,
            Map<String, String> after
    ) {
        // Gộp tất cả keys từ cả 2 phía, 1 pass duy nhất
        Set<String> allKeys = new LinkedHashSet<>(before.keySet());
        allKeys.addAll(after.keySet());

        Map<String, FieldChange> changes = allKeys.stream()
                .filter(k -> !Objects.equals(before.get(k), after.get(k)))
                .collect(Collectors.toMap(
                        k -> k,
                        k -> new FieldChange(before.get(k), after.get(k)),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        if (changes.isEmpty()) return Optional.empty();

        return Optional.of(new DomainChangeEvent(aggregateType, aggregateId, changes, Instant.now()));
    }
}