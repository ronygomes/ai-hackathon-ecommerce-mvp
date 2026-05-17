package me.ronygomes.ecommerce.inventory.application;

import com.google.inject.Inject;
import me.rongyomes.ecommerce.checkout.saga.message.command.DeductStockForOrderCommand;
import me.rongyomes.ecommerce.checkout.saga.message.event.StockDeductionFailed;
import me.ronygomes.ecommerce.core.application.CommandHandler;
import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.core.infrastructure.outbox.OutboxStore;
import me.ronygomes.ecommerce.inventory.domain.InventoryItem;
import me.ronygomes.ecommerce.inventory.domain.ProductId;
import me.ronygomes.ecommerce.inventory.domain.Quantity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DeductStockForOrderHandler implements CommandHandler<DeductStockForOrderCommand, Void> {
    private final Repository<InventoryItem, ProductId> repository;
    private final OutboxStore outboxStore;

    @Inject
    public DeductStockForOrderHandler(Repository<InventoryItem, ProductId> repository, OutboxStore outboxStore) {
        this.repository = repository;
        this.outboxStore = outboxStore;
    }

    @Override
    public CompletableFuture<Void> handle(DeductStockForOrderCommand command) {
        return CompletableFuture.runAsync(() -> {
            for (DeductStockForOrderCommand.StockItemRequest item : command.items()) {
                deductOne(command.orderId(), item);
            }
        });
    }

    private void deductOne(UUID orderId, DeductStockForOrderCommand.StockItemRequest item) {
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
            outboxStore.append(inv.getId().toString(), inv.getUncommittedEvents());
            inv.clearUncommittedEvents();
        } catch (Exception e) {
            publishFailure(orderId, item, 0, "Unexpected error: " + e.getMessage());
        }
    }

    private void publishFailure(UUID orderId, DeductStockForOrderCommand.StockItemRequest item,
                                int availableQty, String reason) {
        outboxStore.append(orderId.toString(), List.of(new StockDeductionFailed(
                orderId, item.productId(), item.qty(), availableQty, reason)));
    }
}
