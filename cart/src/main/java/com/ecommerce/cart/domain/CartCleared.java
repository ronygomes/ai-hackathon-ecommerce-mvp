package com.ecommerce.cart.domain;

import com.ecommerce.core.domain.IDomainEvent;
import java.util.UUID;

public record CartCleared(UUID cartId, String eventId, long timestamp) implements IDomainEvent {
    public CartCleared(UUID cartId) {
        this(cartId, UUID.randomUUID().toString(), System.currentTimeMillis());
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
