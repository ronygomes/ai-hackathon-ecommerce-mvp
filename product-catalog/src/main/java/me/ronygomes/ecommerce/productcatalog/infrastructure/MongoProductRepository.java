package me.ronygomes.ecommerce.productcatalog.infrastructure;

import com.google.inject.Inject;
import com.mongodb.client.MongoClient;
import me.ronygomes.ecommerce.core.infrastructure.BaseMongoRepository;
import me.ronygomes.ecommerce.productcatalog.domain.Product;
import me.ronygomes.ecommerce.productcatalog.domain.ProductId;

public class MongoProductRepository extends BaseMongoRepository<Product, ProductId> {
    @Inject
    public MongoProductRepository(MongoClient mongoClient) {
        super(mongoClient, "aihackathon", "products", Product.class);
    }
}
