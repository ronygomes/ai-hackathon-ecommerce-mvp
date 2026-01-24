package com.ecommerce.ordering.domain;

import com.ecommerce.core.domain.IDomainEvent;
import java.util.UUID;

public record CartClearRequested(String guestToken, String cartId, String eventId, long timestamp)
        implements IDomainEvent {
    public CartClearRequested(String guestToken, String cartId) {
        this(guestToken, cartId, UUID.randomUUID().toString(), System.currentTimeMillis());
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
