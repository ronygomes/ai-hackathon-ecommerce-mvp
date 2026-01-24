package com.ecommerce.inventory.application;

import com.ecommerce.core.application.ICommandHandler;
import com.ecommerce.core.infrastructure.IRepository;
import com.ecommerce.core.messaging.IMessageBus;
import com.ecommerce.inventory.domain.*;
import com.ecommerce.checkout.saga.messages.commands.ValidateStockBatchCommand;
import com.ecommerce.checkout.saga.messages.events.StockBatchValidated;
import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
        return CompletableFuture.runAsync(() -> {
            try {
                boolean allAvailable = true;
                for (com.ecommerce.checkout.saga.messages.commands.ValidateStockBatchCommand.StockItemRequest req : command
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
