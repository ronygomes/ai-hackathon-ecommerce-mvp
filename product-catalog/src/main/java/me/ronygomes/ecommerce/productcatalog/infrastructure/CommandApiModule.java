package me.ronygomes.ecommerce.productcatalog.infrastructure;

import com.google.inject.AbstractModule;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.infrastructure.RabbitMQCommandBus;

public class CommandApiModule extends AbstractModule {

    private static final String PRODUCT_CATALOG_QUEUE_NAME = "product_catalog_commands";

    @Override
    protected void configure() {
        bind(CommandBus.class).toInstance(new RabbitMQCommandBus(PRODUCT_CATALOG_QUEUE_NAME, "localhost"));
    }
}
