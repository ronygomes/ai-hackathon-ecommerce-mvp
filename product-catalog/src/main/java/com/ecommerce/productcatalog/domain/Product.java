package com.ecommerce.productcatalog.domain;

import com.ecommerce.core.domain.BaseAggregate;
import java.util.UUID;

public class Product extends BaseAggregate<UUID> {
    private String name;
    private String sku;
    private double price;
    private String description;

    private Product(UUID id, String name, String sku, double price, String description) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.price = price;
        this.description = description;
    }

    public static Product create(String name, String sku, double price, String description) {
        if (price < 0)
            throw new IllegalArgumentException("Price cannot be negative");
        Product product = new Product(UUID.randomUUID(), name, sku, price, description);
        product.addEvent(new ProductCreatedEvent(product.id, name, sku, price));
        return product;
    }

    public void updateDetails(String name, double price, String description) {
        if (price < 0)
            throw new IllegalArgumentException("Price cannot be negative");
        this.name = name;
        this.price = price;
        this.description = description;
        addEvent(new ProductUpdatedEvent(this.id, name, price));
    }

    // Getters for state persistence
    public String getName() {
        return name;
    }

    public String getSku() {
        return sku;
    }

    public double getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }
}
