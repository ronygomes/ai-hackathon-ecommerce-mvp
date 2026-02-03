package me.ronygomes.ecommerce.ordering.domain;

import me.ronygomes.ecommerce.core.domain.DomainEvent;

import java.util.List;
import java.util.UUID;

public record StockCommitRequested(UUID orderId, List<StockItem> items, String eventId, long timestamp)
        implements DomainEvent {
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
