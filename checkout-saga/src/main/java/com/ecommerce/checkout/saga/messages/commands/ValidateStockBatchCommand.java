package com.ecommerce.checkout.saga.messages.commands;

import com.ecommerce.core.application.ICommand;
import java.util.List;
import java.util.UUID;

public record ValidateStockBatchCommand(List<StockItemRequest> items) implements ICommand<Void> {
    public record StockItemRequest(UUID productId, int qty) {
    }
}
