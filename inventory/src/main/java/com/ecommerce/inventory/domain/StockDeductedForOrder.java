package com.ecommerce.inventory.domain;

import com.ecommerce.core.domain.IDomainEvent;
import java.util.UUID;

public record StockDeductedForOrder(UUID orderId, UUID productId, int qty, int oldQty, int newQty, String eventId,
        long timestamp) implements IDomainEvent {
    public StockDeductedForOrder(UUID orderId, UUID productId, int qty, int oldQty, int newQty) {
        this(orderId, productId, qty, oldQty, newQty, UUID.randomUUID().toString(), System.currentTimeMillis());
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
