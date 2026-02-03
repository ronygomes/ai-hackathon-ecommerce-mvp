package me.ronygomes.ecommerce.ordering.domain;

import me.ronygomes.ecommerce.core.domain.DomainEvent;
import java.util.UUID;

public record OrderStockCommitted(UUID orderId, String eventId, long timestamp) implements DomainEvent {
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
