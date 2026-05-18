package me.ronygomes.ecommerce.productcatalog.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import me.ronygomes.ecommerce.core.application.Command;
import me.ronygomes.ecommerce.productcatalog.domain.ProductId;

import java.util.UUID;

public record CreateProductCommand(
                @NotNull UUID productId,
                @NotBlank(message = "sku cannot be empty") String sku,
                String name,
                double price,
                String description) implements Command<ProductId> {
}
