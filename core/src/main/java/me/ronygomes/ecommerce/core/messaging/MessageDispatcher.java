package me.ronygomes.ecommerce.core.messaging;

import java.util.concurrent.CompletableFuture;

public interface MessageDispatcher {
    <T> CompletableFuture<Void> dispatch(String messageType, String messageData);
}
