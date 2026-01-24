package com.ecommerce.productcatalog.domain;

import com.ecommerce.core.domain.IDomainEvent;
import java.util.UUID;

public record ProductUpdatedEvent(
        UUID productId,
        String name,
        double price,
        String eventId,
        long timestamp) implements IDomainEvent {
    public ProductUpdatedEvent(UUID productId, String name, double price) {
        this(productId, name, price, UUID.randomUUID().toString(), System.currentTimeMillis());
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
