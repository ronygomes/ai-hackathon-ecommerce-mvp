package me.ronygomes.ecommerce.checkout.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.ronygomes.ecommerce.checkout.saga.handler.*;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.domain.DomainEvent;
import me.ronygomes.ecommerce.core.infrastructure.idempotency.ProcessedCommandStore;
import me.ronygomes.ecommerce.core.messaging.*;

import java.util.concurrent.CompletionException;

/**
 * Wires up the saga's per-message-type handlers on a {@link MessageDispatcher} and exposes
 * a synchronous {@link #handle(String, String)} entry point for tests and the
 * {@code CheckoutSagaProcess} consumer loop.
 *
 * <p>Each branch of the previous {@code switch(messageType)} now lives in its own
 * {@code MessageHandler<T>} under {@code saga.handler/}, mirroring how every subsystem
 * registers handlers on a {@link MessageDispatcher}.
 */
public class SagaOrchestrator {

    private final SagaStateStore store;
    private final MessageDispatcher dispatcher;

    public SagaOrchestrator(CommandBus orderBus, CommandBus cartBus, CommandBus catalogBus, CommandBus inventoryBus,
                            ObjectMapper objectMapper, SagaStateStore store) {
        this(orderBus, cartBus, catalogBus, inventoryBus, objectMapper, store, null);
    }

    /**
     * Overload that wires inbound-event idempotency. When {@code processedEventStore} is
     * non-null, each step handler is wrapped in an {@link IdempotentMessageHandler} so
     * duplicate AMQP redeliveries of the same event id won't fire duplicate downstream
     * commands (or mutate saga state twice).
     */
    public SagaOrchestrator(CommandBus orderBus, CommandBus cartBus, CommandBus catalogBus, CommandBus inventoryBus,
                            ObjectMapper objectMapper, SagaStateStore store,
                            ProcessedCommandStore processedEventStore) {
        this.store = store;
        MessageDispatcherImpl impl = new MessageDispatcherImpl(objectMapper);
        impl.registerHandler("CheckoutRequested",
                wrap(new CheckoutRequestedHandler(cartBus, store), processedEventStore));
        impl.registerHandler("CartSnapshotProvided",
                wrap(new CartSnapshotProvidedHandler(catalogBus, store), processedEventStore));
        impl.registerHandler("ProductSnapshotsProvided",
                wrap(new ProductSnapshotsProvidedHandler(inventoryBus, store), processedEventStore));
        impl.registerHandler("StockBatchValidated",
                wrap(new StockBatchValidatedHandler(inventoryBus, store), processedEventStore));
        impl.registerHandler("StockDeductedForOrder",
                wrap(new StockDeductedForOrderHandler(orderBus, store), processedEventStore));
        impl.registerHandler("OrderCreated",
                wrap(new OrderCreatedHandler(cartBus, store), processedEventStore));
        impl.registerHandler("CartCleared",
                wrap(new CartClearedHandler(store), processedEventStore));
        impl.registerHandler("StockBatchValidationFailed",
                wrap(new StockBatchValidationFailedHandler(orderBus, store), processedEventStore));
        impl.registerHandler("StockDeductionFailed",
                wrap(new StockDeductionFailedHandler(orderBus, store), processedEventStore));
        this.dispatcher = impl;
    }

    private static <T extends DomainEvent> MessageHandler<T> wrap(MessageHandler<T> handler,
                                                                  ProcessedCommandStore processedEventStore) {
        return processedEventStore == null ? handler : new IdempotentMessageHandler<>(handler, processedEventStore);
    }

    public SagaStateStore store() {
        return store;
    }

    public MessageDispatcher dispatcher() {
        return dispatcher;
    }

    /**
     * Synchronous entry point preserved for test compatibility and the consumer loop.
     * Unwraps the {@link CompletionException} that {@code CompletableFuture#join()} would
     * otherwise raise so callers see the original cause directly.
     */
    public void handle(String messageType, String message) throws Exception {
        try {
            dispatcher.dispatch(messageType, message, MessageMetadata.empty()).join();
        } catch (CompletionException ce) {
            Throwable cause = ce.getCause();
            if (cause instanceof Exception e) {
                throw e;
            }
            throw ce;
        }
    }
}
