package com.ecommerce.productcatalog.domain;

import com.ecommerce.core.domain.IDomainEvent;
import java.util.UUID;

public record ProductDetailsUpdated(
        ProductId productId,
        ProductName name,
        ProductDescription description,
        String eventId,
        long timestamp) implements IDomainEvent {
    public ProductDetailsUpdated(ProductId productId, ProductName name, ProductDescription description) {
        this(productId, name, description, UUID.randomUUID().toString(), System.currentTimeMillis());
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
