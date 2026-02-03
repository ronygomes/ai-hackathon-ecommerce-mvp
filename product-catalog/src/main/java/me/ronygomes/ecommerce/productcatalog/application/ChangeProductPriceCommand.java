package me.ronygomes.ecommerce.productcatalog.application;

import me.ronygomes.ecommerce.core.application.Command;

import java.util.UUID;

public record ChangeProductPriceCommand(
        UUID productId,
        double newPrice) implements Command<Void> {
}
