package me.ronygomes.ecommerce.ordering.domain;

import me.ronygomes.ecommerce.core.domain.DomainEvent;
import java.util.UUID;

public record CartClearRequested(String guestToken, String cartId, String eventId, long timestamp)
        implements DomainEvent {
    public CartClearRequested(String guestToken, String cartId) {
        this(guestToken, cartId, UUID.randomUUID().toString(), System.currentTimeMillis());
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
