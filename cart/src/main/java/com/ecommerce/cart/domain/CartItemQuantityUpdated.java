package com.ecommerce.cart.domain;

import com.ecommerce.core.domain.IDomainEvent;
import java.util.UUID;

public record CartItemQuantityUpdated(UUID cartId, UUID productId, int oldQty, int newQty, String eventId,
        long timestamp) implements IDomainEvent {
    public CartItemQuantityUpdated(UUID cartId, UUID productId, int oldQty, int newQty) {
        this(cartId, productId, oldQty, newQty, UUID.randomUUID().toString(), System.currentTimeMillis());
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
