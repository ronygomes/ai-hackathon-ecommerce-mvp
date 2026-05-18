package me.ronygomes.ecommerce.checkout.saga.handler;

import me.ronygomes.ecommerce.checkout.saga.SagaState;
import me.ronygomes.ecommerce.checkout.saga.SagaStateStore;
import me.ronygomes.ecommerce.checkout.saga.message.command.GetCartSnapshotCommand;
import me.ronygomes.ecommerce.checkout.saga.message.event.CheckoutRequested;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.messaging.MessageHandler;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CheckoutRequestedHandler implements MessageHandler<CheckoutRequested> {

    private final CommandBus cartBus;
    private final SagaStateStore store;

    public CheckoutRequestedHandler(CommandBus cartBus, SagaStateStore store) {
        this.cartBus = cartBus;
        this.store = store;
    }

    @Override
    public CompletableFuture<Void> handle(CheckoutRequested event) {
        UUID correlationId = UUID.randomUUID();
        SagaState state = new SagaState(event.orderId(), correlationId, event.guestToken(), event.idempotencyKey());
        store.save(state);
        cartBus.send(new GetCartSnapshotCommand(event.guestToken(), correlationId));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Class<CheckoutRequested> getMessageType() {
        return CheckoutRequested.class;
    }
}
