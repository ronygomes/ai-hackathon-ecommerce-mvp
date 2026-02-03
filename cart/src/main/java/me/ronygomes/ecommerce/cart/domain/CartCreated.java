package me.ronygomes.ecommerce.cart.domain;

import me.ronygomes.ecommerce.core.domain.DomainEvent;
import java.util.UUID;

public record CartCreated(UUID cartId, String guestToken, String eventId, long timestamp) implements DomainEvent {
    public CartCreated(UUID cartId, String guestToken) {
        this(cartId, guestToken, UUID.randomUUID().toString(), System.currentTimeMillis());
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
