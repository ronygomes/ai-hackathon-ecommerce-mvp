package me.ronygomes.ecommerce.checkout.saga.handler;

import me.ronygomes.ecommerce.checkout.saga.SagaState;
import me.ronygomes.ecommerce.checkout.saga.SagaStateStore;
import me.ronygomes.ecommerce.checkout.saga.message.event.StockBatchValidationFailed;
import me.ronygomes.ecommerce.core.messaging.MessageHandler;

import java.util.concurrent.CompletableFuture;

public class StockBatchValidationFailedHandler implements MessageHandler<StockBatchValidationFailed> {

    private final SagaStateStore store;

    public StockBatchValidationFailedHandler(SagaStateStore store) {
        this.store = store;
    }

    @Override
    public CompletableFuture<Void> handle(StockBatchValidationFailed event) {
        SagaState state = store.findByCorrelationId(event.correlationId()).orElse(null);
        if (state != null) {
            System.err.println("Saga ABORTED for order " + state.orderId
                    + ": stock validation failed for " + event.rejected().size() + " item(s)");
            store.remove(state.orderId);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Class<StockBatchValidationFailed> getMessageType() {
        return StockBatchValidationFailed.class;
    }
}
