package me.ronygomes.ecommerce.checkout.saga.handler;

import me.ronygomes.ecommerce.checkout.saga.SagaState;
import me.ronygomes.ecommerce.checkout.saga.SagaStateStore;
import me.ronygomes.ecommerce.checkout.saga.message.event.StockBatchValidationFailed;
import me.ronygomes.ecommerce.core.messaging.MessageHandler;
import me.ronygomes.ecommerce.core.observability.MdcScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class StockBatchValidationFailedHandler implements MessageHandler<StockBatchValidationFailed> {

    private static final Logger log = LoggerFactory.getLogger(StockBatchValidationFailedHandler.class);

    private final SagaStateStore store;

    public StockBatchValidationFailedHandler(SagaStateStore store) {
        this.store = store;
    }

    @Override
    public CompletableFuture<Void> handle(StockBatchValidationFailed event) {
        try (var ignored = MdcScope.with("correlationId", event.correlationId().toString())) {
            SagaState state = store.findByCorrelationId(event.correlationId()).orElse(null);
            if (state != null) {
                log.warn("Saga ABORTED for order {}: stock validation failed for {} item(s)",
                        state.orderId, event.rejected().size());
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
