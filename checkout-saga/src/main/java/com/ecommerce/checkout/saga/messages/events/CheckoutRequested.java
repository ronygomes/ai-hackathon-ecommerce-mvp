package com.ecommerce.checkout.saga.messages.events;

import com.ecommerce.core.domain.IDomainEvent;
import java.util.UUID;

public record CheckoutRequested(
        UUID orderId,
        String guestToken,
        String cartId,
        String customerName,
        String customerPhone,
        String customerEmail,
        String addressLine1,
        String addressCity,
        String addressZip,
        String addressCountry,
        String idempotencyKey,
        String eventId,
        long timestamp) implements IDomainEvent {

    public CheckoutRequested(UUID orderId, String guestToken, String cartId,
            String customerName, String customerPhone, String customerEmail,
            String addressLine1, String addressCity, String addressZip, String addressCountry,
            String idempotencyKey) {
        this(orderId, guestToken, cartId, customerName, customerPhone, customerEmail,
                addressLine1, addressCity, addressZip, addressCountry,
                idempotencyKey, UUID.randomUUID().toString(), System.currentTimeMillis());
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
