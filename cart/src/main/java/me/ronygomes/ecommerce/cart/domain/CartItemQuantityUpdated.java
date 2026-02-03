package me.ronygomes.ecommerce.cart.domain;

import me.ronygomes.ecommerce.core.domain.DomainEvent;

import java.util.UUID;

public record CartItemQuantityUpdated(UUID cartId, UUID productId, int oldQty, int newQty, String eventId,
        long timestamp) implements DomainEvent {
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
