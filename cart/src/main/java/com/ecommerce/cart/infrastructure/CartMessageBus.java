package com.ecommerce.cart.infrastructure;

import com.ecommerce.core.infrastructure.RabbitMQMessageBus;
import com.google.inject.Singleton;

@Singleton
public class CartMessageBus extends RabbitMQMessageBus {
    public CartMessageBus() {
        super("cart_events", "localhost");
    }
}
