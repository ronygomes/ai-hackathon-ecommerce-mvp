package me.ronygomes.ecommerce.core.messaging;

import me.ronygomes.ecommerce.core.domain.DomainEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface MessageBus {
    CompletableFuture<Void> publish(List<DomainEvent> events);

    CompletableFuture<Void> publishRaw(List<RawMessage> messages);

    record RawMessage(String type, byte[] payload) {
    }
}
