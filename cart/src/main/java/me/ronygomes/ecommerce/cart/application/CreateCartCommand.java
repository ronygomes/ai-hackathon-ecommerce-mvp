package me.ronygomes.ecommerce.cart.application;

import me.ronygomes.ecommerce.core.application.Command;

public record CreateCartCommand(String guestToken) implements Command<Void> {
}
