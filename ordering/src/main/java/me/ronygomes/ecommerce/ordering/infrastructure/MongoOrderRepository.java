package me.ronygomes.ecommerce.ordering.infrastructure;

import com.google.inject.Inject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.Filters;
import me.ronygomes.ecommerce.core.infrastructure.AppConfig;
import me.ronygomes.ecommerce.core.infrastructure.BaseMongoRepository;
import me.ronygomes.ecommerce.ordering.domain.IdempotencyKey;
import me.ronygomes.ecommerce.ordering.domain.Order;
import me.ronygomes.ecommerce.ordering.domain.OrderId;
import org.bson.Document;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class MongoOrderRepository extends BaseMongoRepository<Order, OrderId> implements OrderRepository {
    @Inject
    public MongoOrderRepository(MongoClient mongoClient, AppConfig config) {
        super(mongoClient, config.mongoDbName(), "orders", Order.class);
    }

    @Override
    public CompletableFuture<Optional<Order>> getByIdempotencyKey(IdempotencyKey idempotencyKey) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = collection.find(Filters.eq("idempotencyKey.value", idempotencyKey.value().toString()))
                    .first();
            if (doc == null)
                return Optional.empty();
            try {
                return Optional.of(objectMapper.readValue(doc.toJson(), Order.class));
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize order", e);
            }
        });
    }
}
