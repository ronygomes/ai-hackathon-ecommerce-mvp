package com.ecommerce.checkout.saga.messages.events;

import com.ecommerce.core.domain.IDomainEvent;
import java.util.UUID;

public record CartCleared(
        String guestToken,
        String eventId,
        long timestamp) implements IDomainEvent {

    public CartCleared(String guestToken) {
        this(guestToken, UUID.randomUUID().toString(), System.currentTimeMillis());
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
