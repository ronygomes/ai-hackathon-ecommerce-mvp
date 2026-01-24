package com.ecommerce.productcatalog.application;

import com.ecommerce.core.application.ICommand;
import java.util.UUID;

public record UpdateProductDetailsCommand(
        UUID productId,
        String name,
        String description) implements ICommand<Void> {
}
