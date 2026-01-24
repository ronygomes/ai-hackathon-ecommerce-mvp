package com.ecommerce.checkout.saga.messages.events;

import com.ecommerce.core.domain.IDomainEvent;
import java.util.UUID;

public record StockBatchValidated(String eventId, long timestamp) implements IDomainEvent {
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
