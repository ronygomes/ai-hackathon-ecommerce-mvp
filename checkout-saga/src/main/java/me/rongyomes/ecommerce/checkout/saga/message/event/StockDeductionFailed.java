package me.rongyomes.ecommerce.checkout.saga.message.event;

import me.ronygomes.ecommerce.core.domain.DomainEvent;

import java.util.UUID;

public record StockDeductionFailed(
        UUID orderId,
        UUID productId,
        int requestedQty,
        int availableQty,
        String reason,
        String eventId,
        long timestamp) implements DomainEvent {

    public StockDeductionFailed(UUID orderId, UUID productId, int requestedQty, int availableQty, String reason) {
        this(orderId, productId, requestedQty, availableQty, reason,
                UUID.randomUUID().toString(), System.currentTimeMillis());
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
