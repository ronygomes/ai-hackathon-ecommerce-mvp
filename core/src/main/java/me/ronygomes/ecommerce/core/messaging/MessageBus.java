package me.ronygomes.ecommerce.core.messaging;

import me.ronygomes.ecommerce.core.domain.DomainEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface MessageBus {
    CompletableFuture<Void> publish(List<DomainEvent> events);
}
