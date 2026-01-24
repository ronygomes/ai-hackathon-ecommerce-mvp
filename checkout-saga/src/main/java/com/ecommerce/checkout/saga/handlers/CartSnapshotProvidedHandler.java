package com.ecommerce.checkout.saga.handlers;

import com.ecommerce.core.application.ICommandBus;
import com.ecommerce.core.messaging.IMessageHandler;
import com.ecommerce.checkout.saga.messages.commands.GetProductSnapshotsCommand;
import com.ecommerce.checkout.saga.messages.events.CartSnapshotProvided;
import com.ecommerce.checkout.saga.SagaState;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CartSnapshotProvidedHandler implements IMessageHandler<CartSnapshotProvided> {
    private final Map<UUID, SagaState> activeSagas;
    private final ICommandBus catalogBus;

    public CartSnapshotProvidedHandler(Map<UUID, SagaState> activeSagas, ICommandBus catalogBus) {
        this.activeSagas = activeSagas;
        this.catalogBus = catalogBus;
    }

    @Override
    public CompletableFuture<Void> handle(CartSnapshotProvided event) {
        SagaState state = activeSagas.values().stream()
                .filter(s -> s.guestToken.equals(event.guestToken()))
                .findFirst().orElse(null);

        if (state != null) {
            state.cartItems = event.items();
            state.totalItemsToDeduct = event.items().size();
            java.util.List<UUID> pids = event.items().stream()
                    .map(CartSnapshotProvided.CartItemSnapshot::productId)
                    .collect(Collectors.toList());
            catalogBus.send(new GetProductSnapshotsCommand(pids));
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Class<CartSnapshotProvided> getMessageType() {
        return CartSnapshotProvided.class;
    }
}
