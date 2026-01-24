package com.ecommerce.core.domain;

import java.util.List;

public interface IAggregateRoot<TId> {
    TId getId();

    Long getVersion();

    List<IDomainEvent> getUncommittedEvents();

    void clearUncommittedEvents();
}
