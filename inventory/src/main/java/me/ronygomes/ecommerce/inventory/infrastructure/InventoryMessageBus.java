package me.ronygomes.ecommerce.inventory.infrastructure;

import com.google.inject.Singleton;
import me.ronygomes.ecommerce.core.infrastructure.RabbitMQMessageBus;

@Singleton
public class InventoryMessageBus extends RabbitMQMessageBus {
    public InventoryMessageBus() {
        super("inventory_events", "localhost");
    }
}
