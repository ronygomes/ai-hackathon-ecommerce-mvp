package me.ronygomes.ecommerce.checkout.saga.handler;

import me.ronygomes.ecommerce.checkout.saga.SagaState;
import me.ronygomes.ecommerce.checkout.saga.SagaStateStore;
import me.ronygomes.ecommerce.checkout.saga.message.event.CartCleared;
import me.ronygomes.ecommerce.core.messaging.MessageHandler;
import me.ronygomes.ecommerce.core.observability.MdcScope;

import java.util.concurrent.CompletableFuture;

public class CartClearedHandler implements MessageHandler<CartCleared> {

    private final SagaStateStore store;

    public CartClearedHandler(SagaStateStore store) {
        this.store = store;
    }

    @Override
    public CompletableFuture<Void> handle(CartCleared event) {
        try (var ignored = MdcScope.with("correlationId", event.correlationId().toString())) {
            SagaState state = store.findByCorrelationId(event.correlationId()).orElse(null);
            if (state != null) {
                store.remove(state.orderId);
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public Class<CartCleared> getMessageType() {
        return CartCleared.class;
    }
}
