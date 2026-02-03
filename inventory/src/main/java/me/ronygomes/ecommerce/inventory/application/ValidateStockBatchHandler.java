package me.ronygomes.ecommerce.inventory.application;

import me.ronygomes.ecommerce.core.application.CommandHandler;
import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
import me.rongyomes.ecommerce.checkout.saga.message.command.ValidateStockBatchCommand;
import me.rongyomes.ecommerce.checkout.saga.message.event.StockBatchValidated;
import com.google.inject.Inject;
import me.ronygomes.ecommerce.inventory.domain.InventoryItem;
import me.ronygomes.ecommerce.inventory.domain.ProductId;
import me.ronygomes.ecommerce.inventory.domain.Quantity;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ValidateStockBatchHandler implements CommandHandler<ValidateStockBatchCommand, Void> {
    private final Repository<InventoryItem, ProductId> repository;
    private final MessageBus messageBus;

    @Inject
    public ValidateStockBatchHandler(Repository<InventoryItem, ProductId> repository, MessageBus messageBus) {
        this.repository = repository;
        this.messageBus = messageBus;
    }

    @Override
    public CompletableFuture<Void> handle(ValidateStockBatchCommand command) {
        return CompletableFuture.runAsync(() -> {
            try {
                boolean allAvailable = true;
                for (ValidateStockBatchCommand.StockItemRequest req : command
                        .items()) {
                    Optional<InventoryItem> item = repository.getById(ProductId.fromString(req.productId().toString()))
                            .get();
                    if (item.isEmpty() || !item.get().isAvailable(new Quantity(req.qty()))) {
                        allAvailable = false;
                        break;
                    }
                }
                if (allAvailable) {
                    messageBus.publish(List.of(new StockBatchValidated()));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
