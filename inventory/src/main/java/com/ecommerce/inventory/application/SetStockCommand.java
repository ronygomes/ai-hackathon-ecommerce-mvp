package com.ecommerce.inventory.application;

import com.ecommerce.core.application.ICommand;
import java.util.UUID;

public record SetStockCommand(UUID productId, int newQty, String reason) implements ICommand<Void> {
}
