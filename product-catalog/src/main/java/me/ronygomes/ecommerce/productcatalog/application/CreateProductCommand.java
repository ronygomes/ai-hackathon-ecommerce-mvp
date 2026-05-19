package me.ronygomes.ecommerce.productcatalog.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import me.ronygomes.ecommerce.core.application.Command;
import me.ronygomes.ecommerce.productcatalog.domain.ProductId;

import java.util.UUID;

public record CreateProductCommand(
                @NotNull UUID productId,
                @NotBlank(message = "sku cannot be empty") String sku,
                @NotBlank(message = "name cannot be empty") String name,
                @Positive(message = "price must be positive") double price,
                String description) implements Command<ProductId> {
}
