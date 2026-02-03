package me.ronygomes.ecommerce.inventory.application;

import me.ronygomes.ecommerce.core.application.Command;
import java.util.UUID;

public record SetStockCommand(UUID productId, int newQty, String reason) implements Command<Void> {
}
