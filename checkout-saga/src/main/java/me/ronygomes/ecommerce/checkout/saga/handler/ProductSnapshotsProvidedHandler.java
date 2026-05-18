package me.ronygomes.ecommerce.checkout.saga.handler;

import me.ronygomes.ecommerce.checkout.saga.SagaState;
import me.ronygomes.ecommerce.checkout.saga.SagaStateStore;
import me.ronygomes.ecommerce.checkout.saga.message.command.ValidateStockBatchCommand;
import me.ronygomes.ecommerce.checkout.saga.message.event.ProductSnapshotsProvided;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.messaging.MessageHandler;
import me.ronygomes.ecommerce.core.observability.MdcScope;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ProductSnapshotsProvidedHandler implements MessageHandler<ProductSnapshotsProvided> {

    private final CommandBus inventoryBus;
    private final SagaStateStore store;

    public ProductSnapshotsProvidedHandler(CommandBus inventoryBus, SagaStateStore store) {
        this.inventoryBus = inventoryBus;
        this.store = store;
    }

    @Override
    public CompletableFuture<Void> handle(ProductSnapshotsProvided event) {
        try (var ignored = MdcScope.with("correlationId", event.correlationId().toString())) {
            SagaState state = store.findByCorrelationId(event.correlationId()).orElse(null);
            if (state != null) {
                state.productSnapshots = event.snapshots();
                store.save(state);
                List<ValidateStockBatchCommand.StockItemRequest> items = state.cartItems.stream()
                        .map(i -> new ValidateStockBatchCommand.StockItemRequest(i.productId(), i.qty()))
                        .collect(Collectors.toList());
                inventoryBus.send(new ValidateStockBatchCommand(items, state.correlationId));
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public Class<ProductSnapshotsProvided> getMessageType() {
        return ProductSnapshotsProvided.class;
    }
}
