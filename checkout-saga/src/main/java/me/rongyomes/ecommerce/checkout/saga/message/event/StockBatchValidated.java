package me.rongyomes.ecommerce.checkout.saga.message.event;

import me.ronygomes.ecommerce.core.domain.DomainEvent;
import java.util.UUID;

public record StockBatchValidated(String eventId, long timestamp) implements DomainEvent {
    public StockBatchValidated() {
        this(UUID.randomUUID().toString(), System.currentTimeMillis());
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
