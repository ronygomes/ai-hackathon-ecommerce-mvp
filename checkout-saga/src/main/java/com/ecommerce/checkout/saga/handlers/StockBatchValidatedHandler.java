package com.ecommerce.checkout.saga.handlers;

import com.ecommerce.core.application.ICommandBus;
import com.ecommerce.core.messaging.IMessageHandler;
import com.ecommerce.checkout.saga.messages.commands.DeductStockForOrderCommand;
import com.ecommerce.checkout.saga.messages.events.StockBatchValidated;
import com.ecommerce.checkout.saga.SagaState;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class StockBatchValidatedHandler implements IMessageHandler<StockBatchValidated> {
    private final Map<UUID, SagaState> activeSagas;
    private final ICommandBus inventoryBus;

    public StockBatchValidatedHandler(Map<UUID, SagaState> activeSagas, ICommandBus inventoryBus) {
        this.activeSagas = activeSagas;
        this.inventoryBus = inventoryBus;
    }

    @Override
    public CompletableFuture<Void> handle(StockBatchValidated event) {
        SagaState state = activeSagas.values().stream()
                .filter(s -> s.productSnapshots != null && s.cartItems != null && !s.stockValidated)
                .findFirst().orElse(null);

        if (state != null) {
            state.stockValidated = true;
            java.util.List<DeductStockForOrderCommand.StockItemRequest> items = state.cartItems.stream()
                    .map(i -> new DeductStockForOrderCommand.StockItemRequest(i.productId(), i.qty()))
                    .collect(Collectors.toList());
            inventoryBus.send(new DeductStockForOrderCommand(state.orderId, items));
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Class<StockBatchValidated> getMessageType() {
        return StockBatchValidated.class;
    }
}
