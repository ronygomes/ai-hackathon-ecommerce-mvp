package me.ronygomes.ecommerce.productcatalog.domain;

import me.ronygomes.ecommerce.core.domain.DomainEvent;
import java.util.UUID;

public record ProductDetailsUpdated(
        ProductId productId,
        ProductName name,
        ProductDescription description,
        String eventId,
        long timestamp) implements DomainEvent {
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
