package com.ecommerce.productcatalog.domain;

import com.ecommerce.core.domain.IDomainEvent;
import java.util.UUID;

public record ProductCreated(
        ProductId productId,
        Sku sku,
        ProductName name,
        Price price,
        ProductDescription description,
        String eventId,
        long timestamp) implements IDomainEvent {
    public ProductCreated(ProductId productId, Sku sku, ProductName name, Price price, ProductDescription description) {
        this(productId, sku, name, price, description, UUID.randomUUID().toString(), System.currentTimeMillis());
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
