package com.ecommerce.inventory.application;

import com.ecommerce.core.application.ICommand;
import java.util.List;
import java.util.UUID;

public record DeductStockForOrderCommand(UUID orderId, List<StockItemRequest> items) implements ICommand<Void> {
    public record StockItemRequest(UUID productId, int qty) {
    }
}
