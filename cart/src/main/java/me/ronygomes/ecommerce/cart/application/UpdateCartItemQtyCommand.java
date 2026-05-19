package me.ronygomes.ecommerce.cart.application;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import me.ronygomes.ecommerce.core.application.Command;

import java.util.UUID;

public record UpdateCartItemQtyCommand(
        @NotBlank(message = "guestToken cannot be empty") String guestToken,
        @NotNull UUID productId,
        @Min(value = 1, message = "qty must be >= 1") int qty) implements Command<Void> {
}
