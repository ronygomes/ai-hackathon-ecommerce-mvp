package me.ronygomes.ecommerce.productcatalog.domain;

import me.ronygomes.ecommerce.core.domain.DomainEvent;

import java.util.UUID;

public record ProductUpdatedEvent(
        UUID productId,
        String name,
        double price,
        String eventId,
        long timestamp) implements DomainEvent {
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
