package me.ronygomes.ecommerce.cart.infrastructure;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.ronygomes.ecommerce.core.infrastructure.AppConfig;
import me.ronygomes.ecommerce.core.infrastructure.RabbitMQMessageBus;

@Singleton
public class CartMessageBus extends RabbitMQMessageBus {
    @Inject
    public CartMessageBus(AppConfig config) {
        super("cart_events", config.rabbitHost());
    }
}
