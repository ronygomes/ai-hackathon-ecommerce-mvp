package com.ecommerce.cart.domain;

import com.ecommerce.core.domain.IDomainEvent;
import java.util.List;
import java.util.UUID;

public record CartSnapshotProvided(
        String guestToken,
        List<CartItemSnapshot> items,
        String eventId,
        long timestamp) implements IDomainEvent {

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
