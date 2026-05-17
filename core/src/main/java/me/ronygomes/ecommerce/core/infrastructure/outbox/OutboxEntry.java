package me.ronygomes.ecommerce.core.infrastructure.outbox;

import java.util.UUID;

public record OutboxEntry(
        String id,
        String aggregateId,
        String eventType,
        String payload,
        long createdAt,
        Long publishedAt) {

    public static OutboxEntry pending(String aggregateId, String eventType, String payload) {
        return new OutboxEntry(
                UUID.randomUUID().toString(),
                aggregateId,
                eventType,
                payload,
                System.currentTimeMillis(),
                null);
    }

    public boolean isPending() {
        return publishedAt == null;
    }
}
