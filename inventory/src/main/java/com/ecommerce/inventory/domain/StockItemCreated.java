package com.ecommerce.inventory.domain;

import com.ecommerce.core.domain.IDomainEvent;
import java.util.UUID;

public record StockItemCreated(UUID productId, int initialQty, String eventId, long timestamp) implements IDomainEvent {
    public StockItemCreated(UUID productId, int initialQty) {
        this(productId, initialQty, UUID.randomUUID().toString(), System.currentTimeMillis());
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
