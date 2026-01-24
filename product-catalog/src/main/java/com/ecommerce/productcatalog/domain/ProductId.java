package com.ecommerce.productcatalog.domain;

import java.util.UUID;

public record ProductId(UUID value) {
    public ProductId {
        if (value == null)
            throw new IllegalArgumentException("ProductId cannot be null");
    }

    public static ProductId generate() {
        return new ProductId(UUID.randomUUID());
    }

    public static ProductId fromString(String value) {
        return new ProductId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
