package me.ronygomes.ecommerce.checkout.saga.message.event;

import me.ronygomes.ecommerce.core.domain.DomainEvent;

import java.util.UUID;

public record CartCleared(
        String guestToken,
        UUID correlationId,
        String causationId,
        String eventId,
        long timestamp) implements DomainEvent {

    public CartCleared(String guestToken, UUID correlationId, String causationId) {
        this(guestToken, correlationId, causationId, UUID.randomUUID().toString(), System.currentTimeMillis());
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
