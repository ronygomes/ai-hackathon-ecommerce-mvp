package com.ecommerce.ordering.domain;

import com.ecommerce.core.domain.IDomainEvent;
import java.util.UUID;

public record OrderPlacementRequested(UUID orderId, String guestToken, String cartId, UUID idempotencyKey,
        String eventId, long timestamp) implements IDomainEvent {
    public OrderPlacementRequested(UUID orderId, String guestToken, String cartId, UUID idempotencyKey) {
        this(orderId, guestToken, cartId, idempotencyKey, UUID.randomUUID().toString(), System.currentTimeMillis());
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
