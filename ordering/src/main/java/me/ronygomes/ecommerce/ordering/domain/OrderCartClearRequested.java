package me.ronygomes.ecommerce.ordering.domain;

import me.ronygomes.ecommerce.core.domain.DomainEvent;
import java.util.UUID;

public record OrderCartClearRequested(UUID orderId, String cartId, String eventId, long timestamp)
        implements DomainEvent {
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
