package com.ecommerce.productcatalog.infrastructure;

import com.ecommerce.core.infrastructure.BaseMongoRepository;
import com.ecommerce.productcatalog.domain.Product;
import com.ecommerce.productcatalog.domain.ProductId;
import com.mongodb.client.MongoClient;
import com.google.inject.Inject;
import java.util.UUID;

public class MongoProductRepository extends BaseMongoRepository<Product, ProductId> {
    @Inject
    public MongoProductRepository(MongoClient mongoClient) {
        super(mongoClient, "aihackathon", "products", Product.class);
    }
}
