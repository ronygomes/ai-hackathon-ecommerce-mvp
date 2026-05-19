package me.ronygomes.ecommerce.core.messaging;

import me.ronygomes.ecommerce.core.domain.DomainEvent;
import me.ronygomes.ecommerce.core.infrastructure.idempotency.ProcessedCommandStore;

import java.util.concurrent.CompletableFuture;

/**
 * Decorates a {@link MessageHandler} with at-most-once delivery semantics, keyed on
 * the event's {@link DomainEvent#getEventId() eventId}. The first time an event id is
 * seen the delegate handler runs and the id is marked as processed on success;
 * subsequent deliveries of the same id short-circuit to a completed future without
 * touching the delegate.
 */
public class IdempotentMessageHandler<T extends DomainEvent> implements MessageHandler<T> {

    private final MessageHandler<T> delegate;
    private final ProcessedCommandStore store;

    public IdempotentMessageHandler(MessageHandler<T> delegate, ProcessedCommandStore store) {
        this.delegate = delegate;
        this.store = store;
    }

    @Override
    public CompletableFuture<Void> handle(T message) {
        return handle(message, MessageMetadata.empty());
    }

    @Override
    public CompletableFuture<Void> handle(T message, MessageMetadata metadata) {
        String eventId = message.getEventId();
        if (eventId == null) {
            return delegate.handle(message, metadata);
        }
        if (store.wasProcessed(eventId)) {
            return CompletableFuture.completedFuture(null);
        }
        return delegate.handle(message, metadata).thenApply(v -> {
            store.markProcessed(eventId, message.getClass().getSimpleName());
            return null;
        });
    }

    @Override
    public Class<T> getMessageType() {
        return delegate.getMessageType();
    }
}
