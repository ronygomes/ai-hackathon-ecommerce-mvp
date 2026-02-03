package me.ronygomes.ecommerce.inventory.application;

import me.ronygomes.ecommerce.core.application.CommandHandler;
import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
import me.rongyomes.ecommerce.checkout.saga.message.command.DeductStockForOrderCommand;
import com.google.inject.Inject;
import me.ronygomes.ecommerce.inventory.domain.InventoryItem;
import me.ronygomes.ecommerce.inventory.domain.ProductId;
import me.ronygomes.ecommerce.inventory.domain.Quantity;

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
            try {
                for (DeductStockForOrderCommand.StockItemRequest item : command
                        .items()) {
                    InventoryItem inv = repository.getById(ProductId.fromString(item.productId().toString())).get()
                            .orElseThrow(
                                    () -> new RuntimeException("Stock not found for Product: " + item.productId()));
                    inv.deductStock(new Quantity(item.qty()), command.orderId().toString());

                    repository.save(inv).get();
                    messageBus.publish(inv.getUncommittedEvents()).get();
                    inv.clearUncommittedEvents();
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to deduct stock", e);
            }
        });
    }
}
