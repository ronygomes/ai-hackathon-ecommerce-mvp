package me.rongyomes.ecommerce.checkout.saga.message.command;

import me.ronygomes.ecommerce.core.application.Command;

import java.util.List;
import java.util.UUID;

public record GetProductSnapshotsCommand(List<UUID> productIds) implements Command<Void> {
}
