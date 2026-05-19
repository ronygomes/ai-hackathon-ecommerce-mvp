package me.ronygomes.ecommerce.checkout.saga.message.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import me.ronygomes.ecommerce.core.application.Command;

import java.util.List;
import java.util.UUID;

public record DeductStockForOrderCommand(
        @NotNull UUID orderId,
        @NotEmpty(message = "items cannot be empty") @Valid List<StockItemRequest> items) implements Command<Void> {
    public record StockItemRequest(
            @NotNull UUID productId,
            @Min(value = 1, message = "qty must be >= 1") int qty) {
    }
}
