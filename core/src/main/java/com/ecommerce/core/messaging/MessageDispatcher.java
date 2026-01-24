package com.ecommerce.core.messaging;

import tools.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MessageDispatcher implements IMessageDispatcher {
    private final Map<String, IMessageHandler<?>> handlers = new HashMap<>();
    private final ObjectMapper objectMapper;

    public MessageDispatcher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> void registerHandler(String messageType, IMessageHandler<T> handler) {
        handlers.put(messageType, handler);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Void> dispatch(String messageType, String messageData) {
        IMessageHandler handler = handlers.get(messageType);
        if (handler == null) {
            System.err.println("No handler registered for message type: " + messageType);
            return CompletableFuture.completedFuture(null);
        }

        try {
            Object message = objectMapper.readValue(messageData, handler.getMessageType());
            return handler.handle(message);
        } catch (Exception e) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
}
