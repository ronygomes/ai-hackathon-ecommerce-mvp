package com.ecommerce.productcatalog.application;

import com.ecommerce.core.application.ICommand;
import java.util.UUID;

public record ChangeProductPriceCommand(
        UUID productId,
        double newPrice) implements ICommand<Void> {
}
