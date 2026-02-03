package me.ronygomes.ecommerce.cart.infrastructure;

import com.google.inject.Singleton;
import me.ronygomes.ecommerce.core.infrastructure.RabbitMQMessageBus;

@Singleton
public class CartMessageBus extends RabbitMQMessageBus {
    public CartMessageBus() {
        super("cart_events", "localhost");
    }
}
