package com.ecommerce.checkout.saga.handlers;

import com.ecommerce.core.application.ICommandBus;
import com.ecommerce.core.messaging.IMessageHandler;
import com.ecommerce.checkout.saga.messages.commands.ClearCartCommand;
import com.ecommerce.checkout.saga.messages.events.OrderCreated;
import com.ecommerce.checkout.saga.SagaState;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SagaOrderCreatedHandler implements IMessageHandler<OrderCreated> {
    private final Map<UUID, SagaState> activeSagas;
    private final ICommandBus cartBus;

    public SagaOrderCreatedHandler(Map<UUID, SagaState> activeSagas, ICommandBus cartBus) {
        this.activeSagas = activeSagas;
        this.cartBus = cartBus;
    }

    @Override
    public CompletableFuture<Void> handle(OrderCreated event) {
        SagaState state = activeSagas.get(UUID.fromString(event.orderId()));
        if (state != null) {
            cartBus.send(new ClearCartCommand(state.guestToken));
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Class<OrderCreated> getMessageType() {
        return OrderCreated.class;
    }
}
