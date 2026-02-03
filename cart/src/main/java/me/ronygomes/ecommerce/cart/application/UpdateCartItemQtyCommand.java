package me.ronygomes.ecommerce.cart.application;

import me.ronygomes.ecommerce.core.application.Command;

import java.util.UUID;

public record UpdateCartItemQtyCommand(String guestToken, UUID productId, int qty) implements Command<Void> {
}
