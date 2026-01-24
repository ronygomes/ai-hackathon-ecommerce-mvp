package com.ecommerce.ordering.infrastructure;

import com.ecommerce.core.infrastructure.RabbitMQMessageBus;
import com.google.inject.Singleton;

@Singleton
public class OrderingMessageBus extends RabbitMQMessageBus {
    public OrderingMessageBus() {
        super("ordering_events", "localhost");
    }
}
