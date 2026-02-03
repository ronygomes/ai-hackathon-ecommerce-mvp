package me.rongyomes.ecommerce.checkout.saga.message.command;

import me.ronygomes.ecommerce.core.application.Command;

import java.util.UUID;

public record MarkCheckoutCompletedCommand(UUID orderId) implements Command<Void> {
}
