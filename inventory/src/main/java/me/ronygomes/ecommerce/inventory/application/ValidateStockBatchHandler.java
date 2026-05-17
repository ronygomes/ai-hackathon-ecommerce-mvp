package me.ronygomes.ecommerce.inventory.application;

import com.google.inject.Inject;
import me.rongyomes.ecommerce.checkout.saga.message.command.ValidateStockBatchCommand;
import me.rongyomes.ecommerce.checkout.saga.message.event.StockBatchValidated;
import me.rongyomes.ecommerce.checkout.saga.message.event.StockBatchValidationFailed;
import me.ronygomes.ecommerce.core.application.CommandHandler;
import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
import me.ronygomes.ecommerce.inventory.domain.InventoryItem;
import me.ronygomes.ecommerce.inventory.domain.ProductId;
import me.ronygomes.ecommerce.inventory.domain.Quantity;

import java.util.ArrayList;
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
                List<StockBatchValidationFailed.RejectedItem> rejected = new ArrayList<>();
                for (ValidateStockBatchCommand.StockItemRequest req : command.items()) {
                    Optional<InventoryItem> item = repository.getById(ProductId.fromString(req.productId().toString()))
                            .get();
                    if (item.isEmpty()) {
                        rejected.add(new StockBatchValidationFailed.RejectedItem(
                                req.productId(), req.qty(), 0, "Stock item not found"));
                    } else if (!item.get().isAvailable(new Quantity(req.qty()))) {
                        rejected.add(new StockBatchValidationFailed.RejectedItem(
                                req.productId(), req.qty(), item.get().getQuantity().value(), "Insufficient stock"));
                    }
                }
                if (rejected.isEmpty()) {
                    messageBus.publish(List.of(new StockBatchValidated()));
                } else {
                    messageBus.publish(List.of(new StockBatchValidationFailed(rejected)));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
