package me.ronygomes.ecommerce.ordering.presentation.eventhandler.handler;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import me.rongyomes.ecommerce.checkout.saga.message.event.OrderCreated;
import me.ronygomes.ecommerce.core.messaging.MessageHandler;
import org.bson.Document;

import java.util.concurrent.CompletableFuture;

import static com.mongodb.client.model.Filters.eq;

public class OrderCreatedProjectionHandler implements MessageHandler<OrderCreated> {
    private final MongoCollection<Document> collection;

    public OrderCreatedProjectionHandler(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    @Override
    public CompletableFuture<Void> handle(OrderCreated event) {
        Document doc = new Document("_id", event.orderId())
                .append("guestToken", event.guestToken())
                .append("status", "COMPLETED") // OrderCreated is fired when finalized in this version
                .append("customerEmail", event.customerEmail());

        collection.replaceOne(eq("_id", event.orderId()), doc, new ReplaceOptions().upsert(true));
        System.out.println("Projected completed order: " + event.orderId());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Class<OrderCreated> getMessageType() {
        return OrderCreated.class;
    }
}
