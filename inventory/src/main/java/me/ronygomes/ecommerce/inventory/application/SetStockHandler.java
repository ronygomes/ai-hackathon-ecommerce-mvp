package me.ronygomes.ecommerce.inventory.application;

import me.ronygomes.ecommerce.core.application.CommandHandler;
import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
import com.google.inject.Inject;
import me.ronygomes.ecommerce.inventory.domain.AdjustmentReason;
import me.ronygomes.ecommerce.inventory.domain.InventoryItem;
import me.ronygomes.ecommerce.inventory.domain.ProductId;
import me.ronygomes.ecommerce.inventory.domain.Quantity;

import java.util.concurrent.CompletableFuture;

public class SetStockHandler implements CommandHandler<SetStockCommand, Void> {
    private final Repository<InventoryItem, ProductId> repository;
    private final MessageBus messageBus;

    @Inject
    public SetStockHandler(Repository<InventoryItem, ProductId> repository, MessageBus messageBus) {
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
