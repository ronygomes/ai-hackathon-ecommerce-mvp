package com.ecommerce.productcatalog.domain;

public record Price(double value) {
    public Price {
        if (value < 0)
            throw new IllegalArgumentException("Price cannot be negative");
    }
}
