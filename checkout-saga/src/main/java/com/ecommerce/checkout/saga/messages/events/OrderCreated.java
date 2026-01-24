package com.ecommerce.checkout.saga.messages.events;

import com.ecommerce.core.domain.IDomainEvent;
import java.util.UUID;

public record OrderCreated(
        String orderId,
        String guestToken,
        String customerEmail,
        String eventId,
        long timestamp) implements IDomainEvent {

    public OrderCreated(String orderId, String guestToken, String customerEmail) {
        this(orderId, guestToken, customerEmail, UUID.randomUUID().toString(), System.currentTimeMillis());
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
