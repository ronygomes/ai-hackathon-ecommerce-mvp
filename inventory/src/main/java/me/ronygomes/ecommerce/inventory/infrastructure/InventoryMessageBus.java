package me.ronygomes.ecommerce.inventory.infrastructure;

import me.ronygomes.ecommerce.core.infrastructure.RabbitMQMessageBus;
import com.google.inject.Singleton;

@Singleton
public class InventoryMessageBus extends RabbitMQMessageBus {
    public InventoryMessageBus() {
        super("inventory_events", "localhost");
    }
}
