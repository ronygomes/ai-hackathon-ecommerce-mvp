package com.ecommerce.cart.infrastructure;

import com.ecommerce.core.infrastructure.BaseMongoRepository;
import com.ecommerce.cart.domain.ShoppingCart;
import com.ecommerce.cart.domain.CartId;
import com.ecommerce.cart.domain.GuestToken;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.Filters;
import com.google.inject.Inject;
import org.bson.Document;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class MongoCartRepository extends BaseMongoRepository<ShoppingCart, CartId> implements ICartRepository {
    @Inject
    public MongoCartRepository(MongoClient mongoClient) {
        super(mongoClient, "aihackathon", "carts", ShoppingCart.class);
    }

    @Override
    public CompletableFuture<Optional<ShoppingCart>> getByGuestToken(GuestToken guestToken) {
        return CompletableFuture.supplyAsync(() -> {
            // Find by guestToken.value since it's a record/value object
            Document doc = collection.find(Filters.eq("guestToken.value", guestToken.value())).first();
            if (doc == null)
                return Optional.empty();
            try {
                return Optional.of(objectMapper.readValue(doc.toJson(), ShoppingCart.class));
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize cart", e);
            }
        });
    }
}
