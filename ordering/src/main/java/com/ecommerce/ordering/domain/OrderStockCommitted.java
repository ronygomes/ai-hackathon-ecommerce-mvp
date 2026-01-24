package com.ecommerce.ordering.domain;

import com.ecommerce.core.domain.IDomainEvent;
import java.util.UUID;

public record OrderStockCommitted(UUID orderId, String eventId, long timestamp) implements IDomainEvent {
    public OrderStockCommitted(UUID orderId) {
        this(orderId, UUID.randomUUID().toString(), System.currentTimeMillis());
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
