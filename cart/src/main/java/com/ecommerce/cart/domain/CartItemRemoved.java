package com.ecommerce.cart.domain;

import com.ecommerce.core.domain.IDomainEvent;
import java.util.UUID;

public record CartItemRemoved(UUID cartId, UUID productId, String eventId, long timestamp) implements IDomainEvent {
    public CartItemRemoved(UUID cartId, UUID productId) {
        this(cartId, productId, UUID.randomUUID().toString(), System.currentTimeMillis());
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
