package me.ronygomes.ecommerce.core.domain;

public interface DomainEvent {
    String getEventId();

    long getTimestamp();
}
