package me.ronygomes.ecommerce.cart.domain;

import me.ronygomes.ecommerce.core.domain.DomainEvent;

import java.util.UUID;

public record CartItemRemoved(UUID cartId, UUID productId, String eventId, long timestamp) implements DomainEvent {
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
