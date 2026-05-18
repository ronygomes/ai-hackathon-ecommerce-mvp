package me.ronygomes.ecommerce.ordering.infrastructure;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.ronygomes.ecommerce.core.infrastructure.AppConfig;
import me.ronygomes.ecommerce.core.infrastructure.RabbitMQMessageBus;

@Singleton
public class OrderingMessageBus extends RabbitMQMessageBus {
    @Inject
    public OrderingMessageBus(AppConfig config) {
        super("ordering_events", config.rabbitHost());
    }
}
