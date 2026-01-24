package com.ecommerce.checkout.saga.handlers;

import com.ecommerce.core.application.ICommandBus;
import com.ecommerce.core.messaging.IMessageHandler;
import com.ecommerce.checkout.saga.messages.commands.MarkCheckoutCompletedCommand;
import com.ecommerce.checkout.saga.messages.events.CartCleared;
import com.ecommerce.checkout.saga.SagaState;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SagaCartClearedHandler implements IMessageHandler<CartCleared> {
    private final Map<UUID, SagaState> activeSagas;
    private final ICommandBus orderBus;

    public SagaCartClearedHandler(Map<UUID, SagaState> activeSagas, ICommandBus orderBus) {
        this.activeSagas = activeSagas;
        this.orderBus = orderBus;
    }

    @Override
    public CompletableFuture<Void> handle(CartCleared event) {
        SagaState state = activeSagas.values().stream()
                .filter(s -> s.guestToken.equals(event.guestToken()))
                .findFirst().orElse(null);

        if (state != null) {
            orderBus.send(new MarkCheckoutCompletedCommand(state.orderId));
            activeSagas.remove(state.orderId);
            System.out.println("Saga SUCCESSFULLY COMPLETED for order: " + state.orderId);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Class<CartCleared> getMessageType() {
        return CartCleared.class;
    }
}
