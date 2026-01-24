package com.ecommerce.core.messaging;

import com.ecommerce.core.domain.IDomainEvent;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface IMessageBus {
    CompletableFuture<Void> publish(List<IDomainEvent> events);
}
