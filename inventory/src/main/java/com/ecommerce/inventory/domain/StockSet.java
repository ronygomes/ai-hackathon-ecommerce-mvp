package com.ecommerce.inventory.domain;

import com.ecommerce.core.domain.IDomainEvent;
import java.util.UUID;

public record StockSet(UUID productId, int oldQty, int newQty, String reason, String changedBy, String eventId,
        long timestamp) implements IDomainEvent {
    public StockSet(UUID productId, int oldQty, int newQty, String reason, String changedBy) {
        this(productId, oldQty, newQty, reason, changedBy, UUID.randomUUID().toString(), System.currentTimeMillis());
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
