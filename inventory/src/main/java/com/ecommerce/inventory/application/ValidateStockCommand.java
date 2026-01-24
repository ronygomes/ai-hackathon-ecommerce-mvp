package com.ecommerce.inventory.application;

import com.ecommerce.core.application.ICommand;
import java.util.List;
import java.util.UUID;

public record ValidateStockCommand(List<StockItemRequest> items) implements ICommand<Boolean> {
    public record StockItemRequest(UUID productId, int requestedQty) {
    }
}
