package com.demo.servicea.messaging;

import com.demo.servicea.messaging.DomainChangeEvent.FieldChange;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DomainChangeDiff {

    private DomainChangeDiff() {
    }

    public static Optional<DomainChangeEvent> buildIfChanged(
            String aggregateType,
            String aggregateId,
            Map<String, String> before,
            Map<String, String> after
    ) {
        Map<String, FieldChange> changes = new LinkedHashMap<>();
        for (String key : after.keySet()) {
            String previous = before.get(key);
            String current = after.get(key);
            if (!Objects.equals(previous, current)) {
                changes.put(key, new FieldChange(previous, current));
            }
        }
        for (String key : before.keySet()) {
            if (!after.containsKey(key)) {
                changes.put(key, new FieldChange(before.get(key), null));
            }
        }
        if (changes.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new DomainChangeEvent(aggregateType, aggregateId, changes, Instant.now()));
    }
}
