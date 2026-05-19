package me.ronygomes.ecommerce.checkout.saga.message.event;

import me.ronygomes.ecommerce.core.domain.DomainEvent;

import java.util.List;
import java.util.UUID;

public record CartSnapshotProvided(
        String guestToken,
        List<CartItemSnapshot> items,
        UUID correlationId,
        String causationId,
        String eventId,
        long timestamp) implements DomainEvent {

    public record CartItemSnapshot(UUID productId, int qty) {
    }

    public CartSnapshotProvided(String guestToken, List<CartItemSnapshot> items, UUID correlationId,
                                String causationId) {
        this(guestToken, items, correlationId, causationId, UUID.randomUUID().toString(),
                System.currentTimeMillis());
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }
}
