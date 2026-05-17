package me.ronygomes.ecommerce.inventory.application;

import com.google.inject.Inject;
import me.rongyomes.ecommerce.checkout.saga.message.command.DeductStockForOrderCommand;
import me.rongyomes.ecommerce.checkout.saga.message.event.StockDeductionFailed;
import me.ronygomes.ecommerce.core.application.CommandHandler;
import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
import me.ronygomes.ecommerce.inventory.domain.InventoryItem;
import me.ronygomes.ecommerce.inventory.domain.ProductId;
import me.ronygomes.ecommerce.inventory.domain.Quantity;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class DeductStockForOrderHandler implements CommandHandler<DeductStockForOrderCommand, Void> {
    private final Repository<InventoryItem, ProductId> repository;
    private final MessageBus messageBus;

    @Inject
    public DeductStockForOrderHandler(Repository<InventoryItem, ProductId> repository, MessageBus messageBus) {
        this.repository = repository;
        this.messageBus = messageBus;
    }

    @Override
    public CompletableFuture<Void> handle(DeductStockForOrderCommand command) {
        return CompletableFuture.runAsync(() -> {
            for (DeductStockForOrderCommand.StockItemRequest item : command.items()) {
                deductOne(command.orderId(), item);
            }
        });
    }

    private void deductOne(java.util.UUID orderId, DeductStockForOrderCommand.StockItemRequest item) {
        try {
            Optional<InventoryItem> found = repository.getById(ProductId.fromString(item.productId().toString())).get();
            if (found.isEmpty()) {
                publishFailure(orderId, item, 0, "Stock item not found");
                return;
            }

            InventoryItem inv = found.get();
            if (!inv.isAvailable(new Quantity(item.qty()))) {
                publishFailure(orderId, item, inv.getQuantity().value(), "Insufficient stock");
                return;
            }

            inv.deductStock(new Quantity(item.qty()), orderId.toString());
            repository.save(inv).get();
            messageBus.publish(inv.getUncommittedEvents()).get();
            inv.clearUncommittedEvents();
        } catch (Exception e) {
            publishFailure(orderId, item, 0, "Unexpected error: " + e.getMessage());
        }
    }

    private void publishFailure(java.util.UUID orderId, DeductStockForOrderCommand.StockItemRequest item,
                                int availableQty, String reason) {
        try {
            messageBus.publish(List.of(new StockDeductionFailed(
                    orderId, item.productId(), item.qty(), availableQty, reason))).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish StockDeductionFailed", e);
        }
    }
}
