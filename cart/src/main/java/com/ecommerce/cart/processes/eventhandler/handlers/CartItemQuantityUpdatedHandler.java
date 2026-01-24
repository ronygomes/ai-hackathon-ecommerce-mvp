package com.ecommerce.cart.processes.eventhandler.handlers;

import com.ecommerce.core.messaging.IMessageHandler;
import com.ecommerce.cart.domain.CartItemQuantityUpdated;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import java.util.concurrent.CompletableFuture;
import static com.mongodb.client.model.Filters.eq;

public class CartItemQuantityUpdatedHandler implements IMessageHandler<CartItemQuantityUpdated> {
    private final MongoCollection<Document> collection;

    public CartItemQuantityUpdatedHandler(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    @Override
    public CompletableFuture<Void> handle(CartItemQuantityUpdated event) {
        collection.updateOne(
                Filters.and(eq("_id", event.cartId().toString()),
                        eq("items.productId", event.productId().toString())),
                Updates.set("items.$.qty", event.newQty()));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Class<CartItemQuantityUpdated> getMessageType() {
        return CartItemQuantityUpdated.class;
    }
}
