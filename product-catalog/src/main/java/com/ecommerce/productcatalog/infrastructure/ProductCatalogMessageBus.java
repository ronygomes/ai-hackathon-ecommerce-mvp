package com.ecommerce.productcatalog.infrastructure;

import com.ecommerce.core.infrastructure.RabbitMQMessageBus;
import com.google.inject.Singleton;

@Singleton
public class ProductCatalogMessageBus extends RabbitMQMessageBus {
    public ProductCatalogMessageBus() {
        super("product_catalog_events", "localhost");
    }
}
