package me.ronygomes.ecommerce.cart.domain;

import me.ronygomes.ecommerce.core.domain.DomainEvent;
import java.util.UUID;

public record CartItemAdded(UUID cartId, UUID productId, int qty, String eventId, long timestamp)
        implements DomainEvent {
    public CartItemAdded(UUID cartId, UUID productId, int qty) {
        this(cartId, productId, qty, UUID.randomUUID().toString(), System.currentTimeMillis());
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
