package com.ecommerce.productcatalog.application;

import com.ecommerce.core.application.ICommand;
import java.util.UUID;

public record DeactivateProductCommand(
        UUID productId) implements ICommand<Void> {
}
