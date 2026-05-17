package me.ronygomes.ecommerce.inventory.application;

import com.google.inject.Inject;
import me.ronygomes.ecommerce.core.application.CommandHandler;
import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.core.infrastructure.outbox.OutboxStore;
import me.ronygomes.ecommerce.inventory.domain.AdjustmentReason;
import me.ronygomes.ecommerce.inventory.domain.InventoryItem;
import me.ronygomes.ecommerce.inventory.domain.ProductId;
import me.ronygomes.ecommerce.inventory.domain.Quantity;

import java.util.concurrent.CompletableFuture;

public class SetStockHandler implements CommandHandler<SetStockCommand, Void> {
    private final Repository<InventoryItem, ProductId> repository;
    private final OutboxStore outboxStore;

    @Inject
    public SetStockHandler(Repository<InventoryItem, ProductId> repository, OutboxStore outboxStore) {
        this.repository = repository;
        this.outboxStore = outboxStore;
    }

    @Override
    public CompletableFuture<Void> handle(SetStockCommand command) {
        ProductId pid = new ProductId(command.productId());
        return repository.getById(pid)
                .thenCompose(opt -> {
                    InventoryItem item = opt.orElseGet(() -> InventoryItem.create(pid, new Quantity(0)));
                    item.setStock(new Quantity(command.newQty()), new AdjustmentReason(command.reason()));

                    return repository.save(item)
                            .thenAccept(v -> {
                                outboxStore.append(item.getId().toString(), item.getUncommittedEvents());
                                item.clearUncommittedEvents();
                            });
                });
    }
}
