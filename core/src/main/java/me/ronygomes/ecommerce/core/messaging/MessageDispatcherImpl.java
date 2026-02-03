package me.ronygomes.ecommerce.core.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MessageDispatcherImpl implements MessageDispatcher {
    private final Map<String, MessageHandler<?>> handlers = new HashMap<>();
    private final ObjectMapper objectMapper;

    public MessageDispatcherImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> void registerHandler(String messageType, MessageHandler<T> handler) {
        handlers.put(messageType, handler);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Void> dispatch(String messageType, String messageData) {
        MessageHandler handler = handlers.get(messageType);
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
