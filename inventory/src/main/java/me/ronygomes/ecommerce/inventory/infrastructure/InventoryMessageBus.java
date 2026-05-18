package me.ronygomes.ecommerce.inventory.infrastructure;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.ronygomes.ecommerce.core.infrastructure.AppConfig;
import me.ronygomes.ecommerce.core.infrastructure.RabbitMQMessageBus;

@Singleton
public class InventoryMessageBus extends RabbitMQMessageBus {
    @Inject
    public InventoryMessageBus(AppConfig config) {
        super("inventory_events", config.rabbitHost());
    }
}
