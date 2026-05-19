package me.ronygomes.ecommerce.checkout.saga.handler;

import me.ronygomes.ecommerce.checkout.saga.SagaState;
import me.ronygomes.ecommerce.checkout.saga.SagaStateStore;
import me.ronygomes.ecommerce.checkout.saga.message.command.DeductStockForOrderCommand;
import me.ronygomes.ecommerce.checkout.saga.message.event.StockBatchValidated;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.messaging.MessageHandler;
import me.ronygomes.ecommerce.core.observability.MdcScope;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class StockBatchValidatedHandler implements MessageHandler<StockBatchValidated> {

    private final CommandBus inventoryBus;
    private final SagaStateStore store;

    public StockBatchValidatedHandler(CommandBus inventoryBus, SagaStateStore store) {
        this.inventoryBus = inventoryBus;
        this.store = store;
    }

    @Override
    public CompletableFuture<Void> handle(StockBatchValidated event) {
        try (var ignored = MdcScope.with(Map.of(
                "correlationId", event.correlationId().toString(),
                "causationId", event.causationId()))) {
            SagaState state = store.findByCorrelationId(event.correlationId()).orElse(null);
            if (state != null) {
                state.stockValidated = true;
                store.save(state);
                List<DeductStockForOrderCommand.StockItemRequest> items = state.cartItems.stream()
                        .map(i -> new DeductStockForOrderCommand.StockItemRequest(i.productId(), i.qty()))
                        .collect(Collectors.toList());
                inventoryBus.send(new DeductStockForOrderCommand(state.orderId, items));
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public Class<StockBatchValidated> getMessageType() {
        return StockBatchValidated.class;
    }
}
