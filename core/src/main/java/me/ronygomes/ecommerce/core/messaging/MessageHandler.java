package me.ronygomes.ecommerce.core.messaging;

import java.util.concurrent.CompletableFuture;

public interface MessageHandler<T> {
    CompletableFuture<Void> handle(T message);

    default CompletableFuture<Void> handle(T message, MessageMetadata metadata) {
        return handle(message);
    }

    Class<T> getMessageType();
}
