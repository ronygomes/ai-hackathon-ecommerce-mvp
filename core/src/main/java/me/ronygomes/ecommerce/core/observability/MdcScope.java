package me.ronygomes.ecommerce.core.observability;

import org.slf4j.MDC;

import java.util.Map;

/**
 * Push a set of MDC entries for the duration of a try-with-resources block; pop them
 * on close. Use at the entry point of message handlers so every log line emitted by
 * the handler (or downstream code on the same thread) carries the relevant correlation
 * context — commandId, correlationId, orderId, etc.
 *
 * <p>Null values are skipped so callers can pass possibly-absent ids without guarding.
 *
 * <pre>{@code
 * try (var ignored = MdcScope.with(Map.of("correlationId", id))) {
 *     // work — every log line gets correlationId=<id>
 * }
 * }</pre>
 */
public final class MdcScope implements AutoCloseable {

    private final Map<String, String> entries;

    private MdcScope(Map<String, String> entries) {
        this.entries = entries;
    }

    public static MdcScope with(Map<String, String> entries) {
        entries.forEach((k, v) -> {
            if (v != null) {
                MDC.put(k, v);
            }
        });
        return new MdcScope(entries);
    }

    public static MdcScope with(String key, String value) {
        // Map.of() throws on null values; skip the entry so callers can pass possibly-null ids.
        return value == null ? with(Map.of()) : with(Map.of(key, value));
    }

    @Override
    public void close() {
        entries.keySet().forEach(MDC::remove);
    }
}
