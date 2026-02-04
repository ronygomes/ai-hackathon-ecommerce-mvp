package me.ronygomes.ecommerce.productcatalog.application;

import jakarta.validation.constraints.NotBlank;
import me.ronygomes.ecommerce.core.application.Command;
import me.ronygomes.ecommerce.productcatalog.domain.ProductId;

public record CreateProductCommand(
                @NotBlank(message = "sku cannot be empty") String sku,
                String name,
                double price,
                String description) implements Command<ProductId> {
}
