package me.ronygomes.ecommerce.productcatalog.infrastructure;

import com.google.inject.Singleton;
import me.ronygomes.ecommerce.core.infrastructure.RabbitMQMessageBus;

@Singleton
public class ProductCatalogMessageBus extends RabbitMQMessageBus {
    public ProductCatalogMessageBus() {
        super("product_catalog_events", "localhost");
    }
}
