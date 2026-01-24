package com.ecommerce.ordering.processes.eventhandler.handlers;

import com.ecommerce.core.messaging.IMessageHandler;
import com.ecommerce.checkout.saga.messages.events.OrderCreated;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import java.util.concurrent.CompletableFuture;
import static com.mongodb.client.model.Filters.eq;

public class OrderCreatedProjectionHandler implements IMessageHandler<OrderCreated> {
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
