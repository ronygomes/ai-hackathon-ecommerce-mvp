package me.ronygomes.ecommerce.productcatalog.application;

import me.ronygomes.ecommerce.core.application.Command;
import me.ronygomes.ecommerce.productcatalog.domain.ProductId;

public record CreateProductCommand(
                String sku,
                String name,
                double price,
                String description) implements Command<ProductId> {
}
