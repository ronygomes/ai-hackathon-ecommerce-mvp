package me.ronygomes.ecommerce.cart.process.eventhandler.handler;

import me.ronygomes.ecommerce.core.messaging.MessageHandler;
import me.ronygomes.ecommerce.cart.domain.CartCreated;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import static com.mongodb.client.model.Filters.eq;

public class CartCreatedHandler implements MessageHandler<CartCreated> {
    private final MongoCollection<Document> collection;

    public CartCreatedHandler(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    @Override
    public CompletableFuture<Void> handle(CartCreated event) {
        Document doc = new Document()
                .append("_id", event.cartId().toString())
                .append("cartId", event.cartId().toString())
                .append("guestToken", event.guestToken())
                .append("items", new ArrayList<>());
        collection.replaceOne(eq("_id", event.cartId().toString()), doc,
                new ReplaceOptions().upsert(true));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Class<CartCreated> getMessageType() {
        return CartCreated.class;
    }
}
