package me.ronygomes.ecommerce.checkout.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.ronygomes.ecommerce.checkout.saga.handler.CartClearedHandler;
import me.ronygomes.ecommerce.checkout.saga.handler.CartSnapshotProvidedHandler;
import me.ronygomes.ecommerce.checkout.saga.handler.CheckoutRequestedHandler;
import me.ronygomes.ecommerce.checkout.saga.handler.OrderCreatedHandler;
import me.ronygomes.ecommerce.checkout.saga.handler.ProductSnapshotsProvidedHandler;
import me.ronygomes.ecommerce.checkout.saga.handler.StockBatchValidatedHandler;
import me.ronygomes.ecommerce.checkout.saga.handler.StockBatchValidationFailedHandler;
import me.ronygomes.ecommerce.checkout.saga.handler.StockDeductedForOrderHandler;
import me.ronygomes.ecommerce.checkout.saga.handler.StockDeductionFailedHandler;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.messaging.MessageDispatcher;
import me.ronygomes.ecommerce.core.messaging.MessageDispatcherImpl;
import me.ronygomes.ecommerce.core.messaging.MessageMetadata;

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
        this.store = store;
        MessageDispatcherImpl impl = new MessageDispatcherImpl(objectMapper);
        impl.registerHandler("CheckoutRequested", new CheckoutRequestedHandler(cartBus, store));
        impl.registerHandler("CartSnapshotProvided", new CartSnapshotProvidedHandler(catalogBus, store));
        impl.registerHandler("ProductSnapshotsProvided", new ProductSnapshotsProvidedHandler(inventoryBus, store));
        impl.registerHandler("StockBatchValidated", new StockBatchValidatedHandler(inventoryBus, store));
        impl.registerHandler("StockDeductedForOrder", new StockDeductedForOrderHandler(orderBus, store));
        impl.registerHandler("OrderCreated", new OrderCreatedHandler(cartBus, store));
        impl.registerHandler("CartCleared", new CartClearedHandler(store));
        impl.registerHandler("StockBatchValidationFailed", new StockBatchValidationFailedHandler(store));
        impl.registerHandler("StockDeductionFailed", new StockDeductionFailedHandler(store));
        this.dispatcher = impl;
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
