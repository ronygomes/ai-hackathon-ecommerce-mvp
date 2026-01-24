package com.ecommerce.inventory.infrastructure;

import com.ecommerce.core.infrastructure.BaseMongoRepository;
import com.ecommerce.inventory.domain.InventoryItem;
import com.ecommerce.inventory.domain.ProductId;
import com.mongodb.client.MongoClient;
import com.google.inject.Inject;

public class MongoInventoryRepository extends BaseMongoRepository<InventoryItem, ProductId> {
    @Inject
    public MongoInventoryRepository(MongoClient mongoClient) {
        super(mongoClient, "aihackathon", "inventory", InventoryItem.class);
    }
}
