package me.ronygomes.ecommerce.cart.application;

import me.ronygomes.ecommerce.core.application.Command;
import java.util.UUID;

public record RemoveCartItemCommand(String guestToken, UUID productId) implements Command<Void> {
}
