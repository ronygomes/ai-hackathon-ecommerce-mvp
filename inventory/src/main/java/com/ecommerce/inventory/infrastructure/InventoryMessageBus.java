package com.ecommerce.inventory.infrastructure;

import com.ecommerce.core.infrastructure.RabbitMQMessageBus;
import com.google.inject.Singleton;

@Singleton
public class InventoryMessageBus extends RabbitMQMessageBus {
    public InventoryMessageBus() {
        super("inventory_events", "localhost");
    }
}
