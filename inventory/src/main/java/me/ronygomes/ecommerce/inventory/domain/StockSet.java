package me.ronygomes.ecommerce.inventory.domain;

import me.ronygomes.ecommerce.core.domain.DomainEvent;
import java.util.UUID;

public record StockSet(UUID productId, int oldQty, int newQty, String reason, String changedBy, String eventId,
        long timestamp) implements DomainEvent {
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
