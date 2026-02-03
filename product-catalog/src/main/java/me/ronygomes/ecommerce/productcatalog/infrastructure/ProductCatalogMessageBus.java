package me.ronygomes.ecommerce.productcatalog.infrastructure;

import me.ronygomes.ecommerce.core.infrastructure.RabbitMQMessageBus;
import com.google.inject.Singleton;

@Singleton
public class ProductCatalogMessageBus extends RabbitMQMessageBus {
    public ProductCatalogMessageBus() {
        super("product_catalog_events", "localhost");
    }
}
