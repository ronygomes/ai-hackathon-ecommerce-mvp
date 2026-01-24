package com.ecommerce.ordering.domain;

import com.ecommerce.core.domain.IDomainEvent;
import java.util.UUID;
import java.util.List;

public record OrderCreated(
        UUID orderId,
        String orderNumber,
        String guestToken,
        CustomerInfo customerInfo,
        ShippingAddress address,
        OrderTotals totals,
        List<OrderLineItem> items,
        String eventId,
        long timestamp) implements IDomainEvent {

    public OrderCreated(UUID orderId, String orderNumber, String guestToken, CustomerInfo customerInfo,
            ShippingAddress address, OrderTotals totals, List<OrderLineItem> items) {
        this(orderId, orderNumber, guestToken, customerInfo, address, totals, items, UUID.randomUUID().toString(),
                System.currentTimeMillis());
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
