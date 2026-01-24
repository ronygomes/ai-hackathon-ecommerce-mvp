package com.ecommerce.core.messaging;

import java.util.concurrent.CompletableFuture;

public interface IMessageDispatcher {
    <T> CompletableFuture<Void> dispatch(String messageType, String messageData);
}
