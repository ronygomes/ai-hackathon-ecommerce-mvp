package me.ronygomes.ecommerce.inventory.application;

import me.ronygomes.ecommerce.core.application.Command;

import java.util.List;
import java.util.UUID;

public record ValidateStockCommand(List<StockItemRequest> items) implements Command<Boolean> {
    public record StockItemRequest(UUID productId, int requestedQty) {
    }
}
