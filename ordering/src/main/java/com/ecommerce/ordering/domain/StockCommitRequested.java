package com.ecommerce.ordering.domain;

import com.ecommerce.core.domain.IDomainEvent;
import java.util.UUID;
import java.util.List;

public record StockCommitRequested(UUID orderId, List<StockItem> items, String eventId, long timestamp)
        implements IDomainEvent {
    public record StockItem(UUID productId, int qty) {
    }

    public StockCommitRequested(UUID orderId, List<StockItem> items) {
        this(orderId, items, UUID.randomUUID().toString(), System.currentTimeMillis());
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
