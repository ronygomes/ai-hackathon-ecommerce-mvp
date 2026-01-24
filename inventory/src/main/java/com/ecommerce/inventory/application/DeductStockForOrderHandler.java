package com.ecommerce.inventory.application;

import com.ecommerce.core.application.ICommandHandler;
import com.ecommerce.core.infrastructure.IRepository;
import com.ecommerce.core.messaging.IMessageBus;
import com.ecommerce.inventory.domain.*;
import com.ecommerce.checkout.saga.messages.commands.DeductStockForOrderCommand;
import com.ecommerce.checkout.saga.messages.events.StockDeductedForOrder;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DeductStockForOrderHandler implements ICommandHandler<DeductStockForOrderCommand, Void> {
    private final IRepository<InventoryItem, ProductId> repository;
    private final IMessageBus messageBus;

    @Inject
    public DeductStockForOrderHandler(IRepository<InventoryItem, ProductId> repository, IMessageBus messageBus) {
        this.repository = repository;
        this.messageBus = messageBus;
    }

    @Override
    public CompletableFuture<Void> handle(DeductStockForOrderCommand command) {
        return CompletableFuture.runAsync(() -> {
            try {
                for (com.ecommerce.checkout.saga.messages.commands.DeductStockForOrderCommand.StockItemRequest item : command
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
