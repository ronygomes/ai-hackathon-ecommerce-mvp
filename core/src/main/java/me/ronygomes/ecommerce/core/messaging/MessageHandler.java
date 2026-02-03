package me.ronygomes.ecommerce.core.messaging;

import java.util.concurrent.CompletableFuture;

public interface MessageHandler<T> {
    CompletableFuture<Void> handle(T message);

    Class<T> getMessageType();
}
