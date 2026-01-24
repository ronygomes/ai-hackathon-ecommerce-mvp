package com.ecommerce.inventory.application;

import com.ecommerce.core.application.ICommandHandler;
import com.ecommerce.core.infrastructure.IRepository;
import com.ecommerce.core.messaging.IMessageBus;
import com.ecommerce.inventory.domain.*;
import com.google.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.stream.Collectors;

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
        // Simple MVP implementation: deduct one by one.
        // In a real system, this should be more transactional or use SAGA.
        List<CompletableFuture<Void>> deductions = command.items().stream()
                .map(req -> repository.getById(new ProductId(req.productId()))
                        .thenCompose(opt -> opt.map(item -> {
                            item.deductForOrder(command.orderId(), new Quantity(req.qty()));
                            return repository.save(item)
                                    .thenCompose(v -> messageBus.publish(item.getUncommittedEvents()))
                                    .thenRun(item::clearUncommittedEvents);
                        }).orElse(CompletableFuture.failedFuture(
                                new RuntimeException("Invemtory item not found for product: " + req.productId())))))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(deductions.toArray(new CompletableFuture[0]));
    }
}
