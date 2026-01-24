package com.ecommerce.productcatalog.application;

import com.ecommerce.core.application.ICommand;
import com.ecommerce.productcatalog.domain.ProductId;

public record CreateProductCommand(
                String sku,
                String name,
                double price,
                String description) implements ICommand<ProductId> {
}
