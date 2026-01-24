package com.ecommerce.inventory.domain;

import com.ecommerce.core.domain.IDomainEvent;
import java.util.UUID;

public record StockDeductionRejected(UUID orderId, UUID productId, int requestedQty, int availableQty, String reason,
        String eventId, long timestamp) implements IDomainEvent {
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
