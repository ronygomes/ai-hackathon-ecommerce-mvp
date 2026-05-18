package me.ronygomes.ecommerce.checkout.saga.message.event;

import me.ronygomes.ecommerce.core.domain.DomainEvent;

import java.util.UUID;

public record OrderCancelled(
        String orderId,
        String reason,
        String eventId,
        long timestamp) implements DomainEvent {

    public OrderCancelled(String orderId, String reason) {
        this(orderId, reason, UUID.randomUUID().toString(), System.currentTimeMillis());
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
