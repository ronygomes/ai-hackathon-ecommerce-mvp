package me.ronygomes.ecommerce.checkout.saga.handler;

import me.ronygomes.ecommerce.checkout.saga.SagaState;
import me.ronygomes.ecommerce.checkout.saga.SagaStateStore;
import me.ronygomes.ecommerce.checkout.saga.message.event.StockDeductionFailed;
import me.ronygomes.ecommerce.core.messaging.MessageHandler;

import java.util.concurrent.CompletableFuture;

public class StockDeductionFailedHandler implements MessageHandler<StockDeductionFailed> {

    private final SagaStateStore store;

    public StockDeductionFailedHandler(SagaStateStore store) {
        this.store = store;
    }

    @Override
    public CompletableFuture<Void> handle(StockDeductionFailed event) {
        SagaState state = store.findByOrderId(event.orderId()).orElse(null);
        if (state != null) {
            System.err.println("Saga ABORTED for order " + state.orderId
                    + ": deduction failed for product " + event.productId() + " (" + event.reason() + ")");
            store.remove(state.orderId);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Class<StockDeductionFailed> getMessageType() {
        return StockDeductionFailed.class;
    }
}
