package me.ronygomes.ecommerce.checkout.saga.message.event;

import me.ronygomes.ecommerce.core.domain.DomainEvent;

import java.util.List;
import java.util.UUID;

public record ProductSnapshotsProvided(
        List<ProductSnapshot> snapshots,
        UUID correlationId,
        String causationId,
        String eventId,
        long timestamp) implements DomainEvent {

    public record ProductSnapshot(UUID productId, String sku, String name, double price, boolean isActive) {
    }

    public ProductSnapshotsProvided(List<ProductSnapshot> snapshots, UUID correlationId, String causationId) {
        this(snapshots, correlationId, causationId, UUID.randomUUID().toString(), System.currentTimeMillis());
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
