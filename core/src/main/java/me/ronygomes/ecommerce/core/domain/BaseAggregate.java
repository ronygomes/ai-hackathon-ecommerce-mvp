package me.ronygomes.ecommerce.core.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BaseAggregate<TId> implements AggregateRoot<TId> {
    protected TId id;
    protected Long version = 0L;
    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();

    @Override
    public TId getId() {
        return id;
    }

    @Override
    public Long getVersion() {
        return version;
    }

    @Override
    public List<DomainEvent> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommittedEvents);
    }

    @Override
    public void clearUncommittedEvents() {
        uncommittedEvents.clear();
    }

    protected void addEvent(DomainEvent event) {
        uncommittedEvents.add(event);
    }
}
