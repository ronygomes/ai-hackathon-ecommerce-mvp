package me.ronygomes.ecommerce.productcatalog.application;

import me.ronygomes.ecommerce.core.application.Command;

import java.util.UUID;

public record UpdateProductDetailsCommand(
        UUID productId,
        String name,
        String description) implements Command<Void> {
}
