package com.ecommerce.cart.processes.eventhandler.handlers;

import com.ecommerce.core.messaging.IMessageHandler;
import com.ecommerce.cart.domain.CartItemRemoved;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import java.util.concurrent.CompletableFuture;
import static com.mongodb.client.model.Filters.eq;

public class CartItemRemovedHandler implements IMessageHandler<CartItemRemoved> {
    private final MongoCollection<Document> collection;

    public CartItemRemovedHandler(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    @Override
    public CompletableFuture<Void> handle(CartItemRemoved event) {
        collection.updateOne(
                eq("_id", event.cartId().toString()),
                Updates.pull("items", new Document("productId", event.productId().toString())));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Class<CartItemRemoved> getMessageType() {
        return CartItemRemoved.class;
    }
}
