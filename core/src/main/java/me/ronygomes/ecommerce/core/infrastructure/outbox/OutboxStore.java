package me.ronygomes.ecommerce.core.infrastructure.outbox;

import me.ronygomes.ecommerce.core.domain.DomainEvent;

import java.util.List;

public interface OutboxStore {

    void append(String aggregateId, List<DomainEvent> events);

    List<OutboxEntry> findPending(int limit);

    void markPublished(List<String> ids);
}
