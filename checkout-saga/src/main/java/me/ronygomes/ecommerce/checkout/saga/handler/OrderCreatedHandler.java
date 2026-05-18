package me.ronygomes.ecommerce.checkout.saga.handler;

import me.ronygomes.ecommerce.checkout.saga.SagaState;
import me.ronygomes.ecommerce.checkout.saga.SagaStateStore;
import me.ronygomes.ecommerce.checkout.saga.message.command.ClearCartCommand;
import me.ronygomes.ecommerce.checkout.saga.message.event.OrderCreated;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.messaging.MessageHandler;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class OrderCreatedHandler implements MessageHandler<OrderCreated> {

    private final CommandBus cartBus;
    private final SagaStateStore store;

    public OrderCreatedHandler(CommandBus cartBus, SagaStateStore store) {
        this.cartBus = cartBus;
        this.store = store;
    }

    @Override
    public CompletableFuture<Void> handle(OrderCreated event) {
        SagaState state = store.findByOrderId(UUID.fromString(event.orderId())).orElse(null);
        if (state != null) {
            cartBus.send(new ClearCartCommand(state.guestToken, state.correlationId));
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Class<OrderCreated> getMessageType() {
        return OrderCreated.class;
    }
}
