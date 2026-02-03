package me.ronygomes.ecommerce.core.domain;

import java.util.List;

public interface AggregateRoot<TId> {
    TId getId();

    Long getVersion();

    List<DomainEvent> getUncommittedEvents();

    void clearUncommittedEvents();
}
