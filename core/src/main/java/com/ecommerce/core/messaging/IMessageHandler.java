package com.ecommerce.core.messaging;

import java.util.concurrent.CompletableFuture;

public interface IMessageHandler<T> {
    CompletableFuture<Void> handle(T message);

    Class<T> getMessageType();
}
