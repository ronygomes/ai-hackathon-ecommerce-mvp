package me.ronygomes.ecommerce.ordering.presentation.eventhandler.handler;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import me.ronygomes.ecommerce.checkout.saga.message.event.OrderCreated;
import me.ronygomes.ecommerce.core.messaging.MessageHandler;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

import static com.mongodb.client.model.Filters.eq;

public class OrderCreatedProjectionHandler implements MessageHandler<OrderCreated> {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedProjectionHandler.class);

    private final MongoCollection<Document> collection;

    public OrderCreatedProjectionHandler(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    @Override
    public CompletableFuture<Void> handle(OrderCreated event) {
        Document doc = new Document("_id", event.orderId())
                .append("guestToken", event.guestToken())
                .append("status", "CONFIRMED") // OrderCreated is fired when finalized in this version
                .append("customerEmail", event.customerEmail());

        collection.replaceOne(eq("_id", event.orderId()), doc, new ReplaceOptions().upsert(true));
        log.info("Projected completed order: {}", event.orderId());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Class<OrderCreated> getMessageType() {
        return OrderCreated.class;
    }
}
