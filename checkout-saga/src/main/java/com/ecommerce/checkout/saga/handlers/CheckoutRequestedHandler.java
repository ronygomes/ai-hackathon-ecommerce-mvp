package com.ecommerce.checkout.saga.handlers;

import com.ecommerce.core.application.ICommandBus;
import com.ecommerce.core.messaging.IMessageHandler;
import com.ecommerce.checkout.saga.messages.commands.GetCartSnapshotCommand;
import com.ecommerce.checkout.saga.messages.events.CheckoutRequested;
import com.ecommerce.checkout.saga.SagaState;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CheckoutRequestedHandler implements IMessageHandler<CheckoutRequested> {
    private final Map<UUID, SagaState> activeSagas;
    private final ICommandBus cartBus;

    public CheckoutRequestedHandler(Map<UUID, SagaState> activeSagas, ICommandBus cartBus) {
        this.activeSagas = activeSagas;
        this.cartBus = cartBus;
    }

    @Override
    public CompletableFuture<Void> handle(CheckoutRequested event) {
        SagaState state = new SagaState(event.orderId(), event.guestToken(), event.idempotencyKey());
        activeSagas.put(event.orderId(), state);
        cartBus.send(new GetCartSnapshotCommand(event.guestToken()));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Class<CheckoutRequested> getMessageType() {
        return CheckoutRequested.class;
    }
}
