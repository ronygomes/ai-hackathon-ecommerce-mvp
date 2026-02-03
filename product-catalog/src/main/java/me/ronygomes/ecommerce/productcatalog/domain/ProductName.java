package me.ronygomes.ecommerce.productcatalog.domain;

public record ProductName(String value) {
    public ProductName {
        if (value == null || value.length() < 2)
            throw new IllegalArgumentException("ProductName must be at least 2 characters");
    }
}
