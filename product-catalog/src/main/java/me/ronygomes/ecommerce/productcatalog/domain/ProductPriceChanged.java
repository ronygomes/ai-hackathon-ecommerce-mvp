package me.ronygomes.ecommerce.productcatalog.domain;

import me.ronygomes.ecommerce.core.domain.DomainEvent;

import java.util.UUID;

public record ProductPriceChanged(
        ProductId productId,
        Price oldPrice,
        Price newPrice,
        String eventId,
        long timestamp) implements DomainEvent {
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
