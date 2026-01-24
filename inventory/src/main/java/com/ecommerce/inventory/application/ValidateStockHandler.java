package com.ecommerce.inventory.application;

import com.ecommerce.core.application.ICommandHandler;
import com.ecommerce.core.infrastructure.IRepository;
import com.ecommerce.inventory.domain.*;
import com.google.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.stream.Collectors;

public class ValidateStockHandler implements ICommandHandler<ValidateStockCommand, Boolean> {
    private final IRepository<InventoryItem, ProductId> repository;

    @Inject
    public ValidateStockHandler(IRepository<InventoryItem, ProductId> repository) {
        this.repository = repository;
    }

    @Override
    public CompletableFuture<Boolean> handle(ValidateStockCommand command) {
        List<CompletableFuture<Boolean>> checks = command.items().stream()
                .map(req -> repository.getById(new ProductId(req.productId()))
                        .thenApply(opt -> opt.map(item -> item.isAvailable(new Quantity(req.requestedQty())))
                                .orElse(false)))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(checks.toArray(new CompletableFuture[0]))
                .thenApply(v -> checks.stream().allMatch(CompletableFuture::join));
    }
}
