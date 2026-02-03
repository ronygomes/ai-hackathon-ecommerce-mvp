package me.ronygomes.ecommerce.inventory.infrastructure;

import me.ronygomes.ecommerce.core.infrastructure.BaseMongoRepository;
import me.ronygomes.ecommerce.inventory.domain.InventoryItem;
import me.ronygomes.ecommerce.inventory.domain.ProductId;
import com.mongodb.client.MongoClient;
import com.google.inject.Inject;

public class MongoInventoryRepository extends BaseMongoRepository<InventoryItem, ProductId> {
    @Inject
    public MongoInventoryRepository(MongoClient mongoClient) {
        super(mongoClient, "aihackathon", "inventory", InventoryItem.class);
    }
}
