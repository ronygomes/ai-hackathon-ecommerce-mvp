package com.ecommerce.core.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BaseAggregate<TId> implements IAggregateRoot<TId> {
    protected TId id;
    protected Long version = 0L;
    private final List<IDomainEvent> uncommittedEvents = new ArrayList<>();

    @Override
    public TId getId() {
        return id;
    }

    @Override
    public Long getVersion() {
        return version;
    }

    @Override
    public List<IDomainEvent> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommittedEvents);
    }

    @Override
    public void clearUncommittedEvents() {
        uncommittedEvents.clear();
    }

    protected void addEvent(IDomainEvent event) {
        uncommittedEvents.add(event);
    }
}
