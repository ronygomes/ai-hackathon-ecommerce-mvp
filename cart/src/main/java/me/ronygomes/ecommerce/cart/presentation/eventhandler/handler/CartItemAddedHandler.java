package me.ronygomes.ecommerce.cart.presentation.eventhandler.handler;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Updates;
import me.ronygomes.ecommerce.cart.domain.CartItemAdded;
import me.ronygomes.ecommerce.core.messaging.MessageHandler;
import org.bson.Document;

import java.util.concurrent.CompletableFuture;

import static com.mongodb.client.model.Filters.eq;

public class CartItemAddedHandler implements MessageHandler<CartItemAdded> {
    private final MongoCollection<Document> collection;

    public CartItemAddedHandler(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    @Override
    public CompletableFuture<Void> handle(CartItemAdded event) {
        Document item = new Document().append("productId", event.productId().toString()).append("qty",
                event.qty());
        collection.updateOne(eq("_id", event.cartId().toString()), Updates.push("items", item));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Class<CartItemAdded> getMessageType() {
        return CartItemAdded.class;
    }
}
