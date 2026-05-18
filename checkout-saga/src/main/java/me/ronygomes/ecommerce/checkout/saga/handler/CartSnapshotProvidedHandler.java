package me.ronygomes.ecommerce.checkout.saga.handler;

import me.ronygomes.ecommerce.checkout.saga.SagaState;
import me.ronygomes.ecommerce.checkout.saga.SagaStateStore;
import me.ronygomes.ecommerce.checkout.saga.message.command.GetProductSnapshotsCommand;
import me.ronygomes.ecommerce.checkout.saga.message.event.CartSnapshotProvided;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.messaging.MessageHandler;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CartSnapshotProvidedHandler implements MessageHandler<CartSnapshotProvided> {

    private final CommandBus catalogBus;
    private final SagaStateStore store;

    public CartSnapshotProvidedHandler(CommandBus catalogBus, SagaStateStore store) {
        this.catalogBus = catalogBus;
        this.store = store;
    }

    @Override
    public CompletableFuture<Void> handle(CartSnapshotProvided event) {
        SagaState state = store.findByCorrelationId(event.correlationId()).orElse(null);
        if (state != null) {
            state.cartItems = event.items();
            state.totalItemsToDeduct = event.items().size();
            store.save(state);
            List<UUID> pids = event.items().stream()
                    .map(CartSnapshotProvided.CartItemSnapshot::productId)
                    .collect(Collectors.toList());
            catalogBus.send(new GetProductSnapshotsCommand(pids, state.correlationId));
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Class<CartSnapshotProvided> getMessageType() {
        return CartSnapshotProvided.class;
    }
}
