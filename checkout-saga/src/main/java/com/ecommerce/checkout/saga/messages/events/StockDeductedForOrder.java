package com.ecommerce.checkout.saga.messages.events;

import com.ecommerce.core.domain.IDomainEvent;
import java.util.UUID;

public record StockDeductedForOrder(
        UUID orderId,
        UUID productId,
        int newQty,
        String eventId,
        long timestamp) implements IDomainEvent {

    public StockDeductedForOrder(UUID orderId, UUID productId, int newQty) {
        this(orderId, productId, newQty, UUID.randomUUID().toString(), System.currentTimeMillis());
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
