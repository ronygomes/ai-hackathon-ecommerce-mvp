package me.ronygomes.ecommerce.productcatalog.infrastructure;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.ronygomes.ecommerce.core.infrastructure.AppConfig;
import me.ronygomes.ecommerce.core.infrastructure.RabbitMQMessageBus;

@Singleton
public class ProductCatalogMessageBus extends RabbitMQMessageBus {
    @Inject
    public ProductCatalogMessageBus(AppConfig config) {
        super("product_catalog_events", config.rabbitHost());
    }
}
