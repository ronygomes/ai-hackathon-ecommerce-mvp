package me.ronygomes.ecommerce.ordering.infrastructure;

import com.google.inject.Singleton;
import me.ronygomes.ecommerce.core.infrastructure.RabbitMQMessageBus;

@Singleton
public class OrderingMessageBus extends RabbitMQMessageBus {
    public OrderingMessageBus() {
        super("ordering_events", "localhost");
    }
}
