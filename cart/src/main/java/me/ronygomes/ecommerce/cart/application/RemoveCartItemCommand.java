package me.ronygomes.ecommerce.cart.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import me.ronygomes.ecommerce.core.application.Command;

import java.util.UUID;

public record RemoveCartItemCommand(
        @NotBlank(message = "guestToken cannot be empty") String guestToken,
        @NotNull UUID productId) implements Command<Void> {
}
