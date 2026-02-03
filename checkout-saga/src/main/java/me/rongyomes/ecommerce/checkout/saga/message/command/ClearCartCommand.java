package me.rongyomes.ecommerce.checkout.saga.message.command;

import me.ronygomes.ecommerce.core.application.Command;

public record ClearCartCommand(String guestToken) implements Command<Void> {
}
