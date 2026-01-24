package com.ecommerce.productcatalog.domain;

import com.ecommerce.core.domain.IDomainEvent;
import java.util.UUID;

public record ProductCreatedEvent(
        UUID productId,
        String name,
        String sku,
        double price,
        String eventId,
        long timestamp) implements IDomainEvent {
    public ProductCreatedEvent(UUID productId, String name, String sku, double price) {
        this(productId, name, sku, price, UUID.randomUUID().toString(), System.currentTimeMillis());
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
