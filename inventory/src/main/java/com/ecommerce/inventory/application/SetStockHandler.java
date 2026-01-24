package com.ecommerce.inventory.application;

import com.ecommerce.core.application.ICommandHandler;
import com.ecommerce.core.infrastructure.IRepository;
import com.ecommerce.core.messaging.IMessageBus;
import com.ecommerce.inventory.domain.*;
import com.google.inject.Inject;
import java.util.concurrent.CompletableFuture;

public class SetStockHandler implements ICommandHandler<SetStockCommand, Void> {
    private final IRepository<InventoryItem, ProductId> repository;
    private final IMessageBus messageBus;

    @Inject
    public SetStockHandler(IRepository<InventoryItem, ProductId> repository, IMessageBus messageBus) {
        this.repository = repository;
        this.messageBus = messageBus;
    }

    @Override
    public CompletableFuture<Void> handle(SetStockCommand command) {
        ProductId pid = new ProductId(command.productId());
        return repository.getById(pid)
                .thenCompose(opt -> {
                    InventoryItem item = opt.orElseGet(() -> InventoryItem.create(pid, new Quantity(0)));
                    item.setStock(new Quantity(command.newQty()), new AdjustmentReason(command.reason()));

                    return repository.save(item)
                            .thenCompose(v -> messageBus.publish(item.getUncommittedEvents()))
                            .thenRun(item::clearUncommittedEvents);
                });
    }
}
