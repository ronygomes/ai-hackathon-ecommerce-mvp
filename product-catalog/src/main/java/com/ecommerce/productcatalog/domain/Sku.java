package com.ecommerce.productcatalog.domain;

public record Sku(String value) {
    public Sku {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("SKU cannot be empty");
    }
}
