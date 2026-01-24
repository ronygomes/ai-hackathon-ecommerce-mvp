package com.ecommerce.checkout.saga.handlers;

import com.ecommerce.core.application.ICommandBus;
import com.ecommerce.core.messaging.IMessageHandler;
import com.ecommerce.checkout.saga.messages.commands.ValidateStockBatchCommand;
import com.ecommerce.checkout.saga.messages.events.ProductSnapshotsProvided;
import com.ecommerce.checkout.saga.SagaState;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ProductSnapshotsProvidedHandler implements IMessageHandler<ProductSnapshotsProvided> {
    private final Map<UUID, SagaState> activeSagas;
    private final ICommandBus inventoryBus;

    public ProductSnapshotsProvidedHandler(Map<UUID, SagaState> activeSagas, ICommandBus inventoryBus) {
        this.activeSagas = activeSagas;
        this.inventoryBus = inventoryBus;
    }

    @Override
    public CompletableFuture<Void> handle(ProductSnapshotsProvided event) {
        SagaState state = activeSagas.values().stream()
                .filter(s -> s.cartItems != null && s.productSnapshots == null)
                .findFirst().orElse(null);

        if (state != null) {
            state.productSnapshots = event.snapshots();
            java.util.List<ValidateStockBatchCommand.StockItemRequest> items = state.cartItems.stream()
                    .map(i -> new ValidateStockBatchCommand.StockItemRequest(i.productId(), i.qty()))
                    .collect(Collectors.toList());
            inventoryBus.send(new ValidateStockBatchCommand(items));
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Class<ProductSnapshotsProvided> getMessageType() {
        return ProductSnapshotsProvided.class;
    }
}
