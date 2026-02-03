package me.ronygomes.ecommerce.inventory.domain;

import me.ronygomes.ecommerce.core.domain.DomainEvent;

import java.util.UUID;

public record StockDeductionRejected(UUID orderId, UUID productId, int requestedQty, int availableQty, String reason,
        String eventId, long timestamp) implements DomainEvent {
    public StockDeductionRejected(UUID orderId, UUID productId, int requestedQty, int availableQty, String reason) {
        this(orderId, productId, requestedQty, availableQty, reason, UUID.randomUUID().toString(),
                System.currentTimeMillis());
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
