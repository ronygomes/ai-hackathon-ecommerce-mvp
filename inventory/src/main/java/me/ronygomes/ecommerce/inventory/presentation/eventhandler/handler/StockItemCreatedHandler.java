package me.ronygomes.ecommerce.inventory.presentation.eventhandler.handler;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import me.ronygomes.ecommerce.core.messaging.MessageHandler;
import me.ronygomes.ecommerce.inventory.domain.StockItemCreated;
import org.bson.Document;

import java.util.concurrent.CompletableFuture;

import static com.mongodb.client.model.Filters.eq;

public class StockItemCreatedHandler implements MessageHandler<StockItemCreated> {
    private final MongoCollection<Document> collection;

    public StockItemCreatedHandler(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    @Override
    public CompletableFuture<Void> handle(StockItemCreated event) {
        String pid = event.productId().toString();
        Document stockDoc = new Document()
                .append("_id", pid)
                .append("productId", pid)
                .append("availableQty", event.initialQty())
                .append("inStockFlag", event.initialQty() > 0);

        collection.replaceOne(eq("_id", pid), stockDoc, new ReplaceOptions().upsert(true));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Class<StockItemCreated> getMessageType() {
        return StockItemCreated.class;
    }
}
