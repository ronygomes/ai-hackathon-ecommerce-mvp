package com.ecommerce.cart.domain;

import com.ecommerce.core.domain.IDomainEvent;
import java.util.UUID;

public record CartCreated(UUID cartId, String guestToken, String eventId, long timestamp) implements IDomainEvent {
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
