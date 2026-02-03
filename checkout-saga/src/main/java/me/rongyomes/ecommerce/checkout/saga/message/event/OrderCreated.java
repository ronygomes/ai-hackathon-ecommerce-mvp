package me.rongyomes.ecommerce.checkout.saga.message.event;

import me.ronygomes.ecommerce.core.domain.DomainEvent;
import java.util.UUID;

public record OrderCreated(
        String orderId,
        String guestToken,
        String customerEmail,
        String eventId,
        long timestamp) implements DomainEvent {

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
