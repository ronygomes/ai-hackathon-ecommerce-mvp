package com.ecommerce.ordering.domain;

import com.ecommerce.core.domain.IDomainEvent;
import java.util.UUID;

public record OrderCartClearRequested(UUID orderId, String cartId, String eventId, long timestamp)
        implements IDomainEvent {
    public OrderCartClearRequested(UUID orderId, String cartId) {
        this(orderId, cartId, UUID.randomUUID().toString(), System.currentTimeMillis());
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
