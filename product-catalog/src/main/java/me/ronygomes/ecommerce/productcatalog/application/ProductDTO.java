package me.ronygomes.ecommerce.productcatalog.application;

import java.util.UUID;

public record ProductDTO(
        UUID id,
        String name,
        String sku,
        double price,
        String description) {
}
