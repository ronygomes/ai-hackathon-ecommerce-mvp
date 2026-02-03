package me.rongyomes.ecommerce.checkout.saga.message.command;

import me.ronygomes.ecommerce.core.application.Command;
import java.util.List;
import java.util.UUID;

public record ValidateStockBatchCommand(List<StockItemRequest> items) implements Command<Void> {
    public record StockItemRequest(UUID productId, int qty) {
    }
}
