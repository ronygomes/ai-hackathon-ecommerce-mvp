package com.ecommerce.productcatalog.domain;

import com.ecommerce.core.domain.IDomainEvent;
import java.util.UUID;

public record ProductActivated(
        ProductId productId,
        String eventId,
        long timestamp) implements IDomainEvent {
    public ProductActivated(ProductId productId) {
        this(productId, UUID.randomUUID().toString(), System.currentTimeMillis());
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
