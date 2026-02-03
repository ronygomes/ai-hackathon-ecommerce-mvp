package me.rongyomes.ecommerce.checkout.saga.message.command;

import me.ronygomes.ecommerce.core.application.Command;

public record GetCartSnapshotCommand(String guestToken) implements Command<Void> {
}
