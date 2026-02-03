package me.ronygomes.ecommerce.cart.process.eventhandler.handler;

import me.ronygomes.ecommerce.core.messaging.MessageHandler;
import me.rongyomes.ecommerce.checkout.saga.message.event.CartCleared;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import static com.mongodb.client.model.Filters.eq;

public class CartClearedEventProjectionHandler implements MessageHandler<CartCleared> {
    private final MongoCollection<Document> collection;

    public CartClearedEventProjectionHandler(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    @Override
    public CompletableFuture<Void> handle(CartCleared event) {
        collection.updateOne(eq("_id", event.guestToken()),
                Updates.set("items", new ArrayList<>()));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Class<CartCleared> getMessageType() {
        return CartCleared.class;
    }
}
