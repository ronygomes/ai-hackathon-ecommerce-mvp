package com.ecommerce.ordering.domain;

import com.ecommerce.core.domain.IDomainEvent;
import java.util.UUID;
import java.util.List;

public record OrderSubmitted(UUID orderId, String orderNumber, String guestToken, List<OrderLineItem> items,
        OrderTotals totals, String eventId, long timestamp) implements IDomainEvent {
    public OrderSubmitted(UUID orderId, String orderNumber, String guestToken, List<OrderLineItem> items,
            OrderTotals totals) {
        this(orderId, orderNumber, guestToken, items, totals, UUID.randomUUID().toString(), System.currentTimeMillis());
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
