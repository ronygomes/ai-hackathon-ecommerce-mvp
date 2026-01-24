package com.ecommerce.productcatalog.domain;

import com.ecommerce.core.domain.IDomainEvent;
import java.util.UUID;

public record ProductPriceChanged(
        ProductId productId,
        Price oldPrice,
        Price newPrice,
        String eventId,
        long timestamp) implements IDomainEvent {
    public ProductPriceChanged(ProductId productId, Price oldPrice, Price newPrice) {
        this(productId, oldPrice, newPrice, UUID.randomUUID().toString(), System.currentTimeMillis());
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
