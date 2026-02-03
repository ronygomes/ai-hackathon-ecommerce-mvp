package me.rongyomes.ecommerce.checkout.saga.message.event;

import me.ronygomes.ecommerce.core.domain.DomainEvent;
import java.util.List;
import java.util.UUID;

public record CartSnapshotProvided(
        String guestToken,
        List<CartItemSnapshot> items,
        String eventId,
        long timestamp) implements DomainEvent {

    public record CartItemSnapshot(UUID productId, int qty) {
    }

    public CartSnapshotProvided(String guestToken, List<CartItemSnapshot> items) {
        this(guestToken, items, UUID.randomUUID().toString(), System.currentTimeMillis());
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
