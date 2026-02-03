package me.ronygomes.ecommerce.productcatalog.application;

import me.ronygomes.ecommerce.core.application.Command;

import java.util.UUID;

public record ActivateProductCommand(
        UUID productId) implements Command<Void> {
}
