package com.ecommerce.productcatalog.application;

import com.ecommerce.core.application.ICommand;
import java.util.UUID;

public record CreateProductCommand(
        String name,
        String sku,
        double price,
        String description) implements ICommand<UUID> {
}
