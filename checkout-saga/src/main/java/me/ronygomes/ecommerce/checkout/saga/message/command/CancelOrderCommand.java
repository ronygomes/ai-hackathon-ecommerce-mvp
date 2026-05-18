package me.ronygomes.ecommerce.checkout.saga.message.command;

import me.ronygomes.ecommerce.core.application.Command;

import java.util.UUID;

public record CancelOrderCommand(UUID orderId, String reason) implements Command<Void> {
}
