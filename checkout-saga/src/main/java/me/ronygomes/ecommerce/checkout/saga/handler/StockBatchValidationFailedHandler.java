package me.ronygomes.ecommerce.checkout.saga.handler;

import me.ronygomes.ecommerce.checkout.saga.SagaState;
import me.ronygomes.ecommerce.checkout.saga.SagaStateStore;
import me.ronygomes.ecommerce.checkout.saga.message.command.CancelOrderCommand;
import me.ronygomes.ecommerce.checkout.saga.message.event.StockBatchValidationFailed;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.messaging.MessageHandler;
import me.ronygomes.ecommerce.core.observability.MdcScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class StockBatchValidationFailedHandler implements MessageHandler<StockBatchValidationFailed> {

    private static final Logger log = LoggerFactory.getLogger(StockBatchValidationFailedHandler.class);

    private final CommandBus orderBus;
    private final SagaStateStore store;

    public StockBatchValidationFailedHandler(CommandBus orderBus, SagaStateStore store) {
        this.orderBus = orderBus;
        this.store = store;
    }

    @Override
    public CompletableFuture<Void> handle(StockBatchValidationFailed event) {
        try (var ignored = MdcScope.with(java.util.Map.of(
                "correlationId", event.correlationId().toString(),
                "causationId", event.causationId()))) {
            SagaState state = store.findByCorrelationId(event.correlationId()).orElse(null);
            if (state != null) {
                String reason = "stock validation failed for " + event.rejected().size() + " item(s)";
                log.warn("Saga ABORTED for order {}: {}", state.orderId, reason);
                orderBus.send(new CancelOrderCommand(state.orderId, reason));
                store.remove(state.orderId);
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public Class<StockBatchValidationFailed> getMessageType() {
        return StockBatchValidationFailed.class;
    }
}
