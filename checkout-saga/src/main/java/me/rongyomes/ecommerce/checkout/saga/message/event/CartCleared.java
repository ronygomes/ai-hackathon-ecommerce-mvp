package me.rongyomes.ecommerce.checkout.saga.message.event;

import me.ronygomes.ecommerce.core.domain.DomainEvent;

import java.util.UUID;

public record CartCleared(
        String guestToken,
        String eventId,
        long timestamp) implements DomainEvent {

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
