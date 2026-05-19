package me.ronygomes.ecommerce.core.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.ronygomes.ecommerce.core.observability.MdcScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MessageDispatcherImpl implements MessageDispatcher {

    private static final Logger log = LoggerFactory.getLogger(MessageDispatcherImpl.class);

    private final Map<String, MessageHandler<?>> handlers = new HashMap<>();
    private final ObjectMapper objectMapper;

    public MessageDispatcherImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> void registerHandler(String messageType, MessageHandler<T> handler) {
        handlers.put(messageType, handler);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public CompletableFuture<Void> dispatch(String messageType, String messageData, MessageMetadata metadata) {
        MessageHandler handler = handlers.get(messageType);
        if (handler == null) {
            log.warn("No handler registered for message type: {}", messageType);
            return CompletableFuture.completedFuture(null);
        }

        // Push commandId (if present) into MDC so any log line emitted by the handler
        // — including ones in downstream library code on this thread — carries the
        // command correlation context. Saga step handlers push their own MDC keys
        // (correlationId/orderId) inside the handler body.
        String commandId = metadata == null ? null : metadata.commandId();
        try (var ignored = MdcScope.with("commandId", commandId)) {
            try {
                Object message = objectMapper.readValue(messageData, handler.getMessageType());
                return handler.handle(message, metadata);
            } catch (Exception e) {
                CompletableFuture<Void> future = new CompletableFuture<>();
                future.completeExceptionally(e);
                return future;
            }
        }
    }
}
