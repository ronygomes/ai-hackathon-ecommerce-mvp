package me.ronygomes.ecommerce.productcatalog.domain;

import me.ronygomes.ecommerce.core.domain.DomainEvent;
import java.util.UUID;

public record ProductActivated(
        ProductId productId,
        String eventId,
        long timestamp) implements DomainEvent {
    public ProductActivated(ProductId productId) {
        this(productId, UUID.randomUUID().toString(), System.currentTimeMillis());
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
