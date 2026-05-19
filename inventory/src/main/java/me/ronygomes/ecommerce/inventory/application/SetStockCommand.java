package me.ronygomes.ecommerce.inventory.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import me.ronygomes.ecommerce.core.application.Command;

import java.util.UUID;

public record SetStockCommand(
        @NotNull UUID productId,
        @PositiveOrZero(message = "newQty must be >= 0") int newQty,
        @NotBlank(message = "reason cannot be empty") String reason) implements Command<Void> {
}
