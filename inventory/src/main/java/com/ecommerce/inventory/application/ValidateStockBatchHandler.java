package com.ecommerce.inventory.application;

import com.ecommerce.core.application.ICommandHandler;
import com.ecommerce.core.infrastructure.IRepository;
import com.ecommerce.core.messaging.IMessageBus;
import com.ecommerce.inventory.domain.*;
import com.google.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ValidateStockBatchHandler implements ICommandHandler<ValidateStockBatchCommand, Void> {
    private final IRepository<InventoryItem, ProductId> repository;
    private final IMessageBus messageBus;

    @Inject
    public ValidateStockBatchHandler(IRepository<InventoryItem, ProductId> repository, IMessageBus messageBus) {
        this.repository = repository;
        this.messageBus = messageBus;
    }

    @Override
    public CompletableFuture<Void> handle(ValidateStockBatchCommand command) {
        List<CompletableFuture<Boolean>> checks = command.items().stream()
                .map(req -> repository.getById(new ProductId(req.productId()))
                        .thenApply(opt -> opt.map(item -> item.isAvailable(new Quantity(req.qty()))).orElse(false)))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(checks.toArray(new CompletableFuture[0]))
                .thenCompose(v -> {
                    boolean allAvailable = checks.stream().allMatch(CompletableFuture::join);
                    if (allAvailable) {
                        return messageBus.publish(List.of(new StockBatchValidated()));
                    } else {
                        throw new RuntimeException("Stock validation failed for one or more items");
                    }
                });
    }
}
