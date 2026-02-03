package me.rongyomes.ecommerce.checkout.saga.message.event;

import me.ronygomes.ecommerce.core.domain.DomainEvent;

import java.util.UUID;

public record StockDeductedForOrder(
        UUID orderId,
        UUID productId,
        int newQty,
        String eventId,
        long timestamp) implements DomainEvent {

    public StockDeductedForOrder(UUID orderId, UUID productId, int newQty) {
        this(orderId, productId, newQty, UUID.randomUUID().toString(), System.currentTimeMillis());
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
