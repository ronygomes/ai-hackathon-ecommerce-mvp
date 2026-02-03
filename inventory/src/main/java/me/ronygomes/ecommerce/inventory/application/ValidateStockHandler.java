package me.ronygomes.ecommerce.inventory.application;

import me.ronygomes.ecommerce.core.application.CommandHandler;
import me.ronygomes.ecommerce.core.infrastructure.Repository;
import com.google.inject.Inject;
import me.ronygomes.ecommerce.inventory.domain.InventoryItem;
import me.ronygomes.ecommerce.inventory.domain.ProductId;
import me.ronygomes.ecommerce.inventory.domain.Quantity;

import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.stream.Collectors;

public class ValidateStockHandler implements CommandHandler<ValidateStockCommand, Boolean> {
    private final Repository<InventoryItem, ProductId> repository;

    @Inject
    public ValidateStockHandler(Repository<InventoryItem, ProductId> repository) {
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
