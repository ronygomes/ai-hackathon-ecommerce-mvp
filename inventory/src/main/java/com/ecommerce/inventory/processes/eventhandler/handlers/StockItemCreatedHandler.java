package com.ecommerce.inventory.processes.eventhandler.handlers;

import com.ecommerce.core.messaging.IMessageHandler;
import com.ecommerce.inventory.domain.StockItemCreated;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import java.util.concurrent.CompletableFuture;
import static com.mongodb.client.model.Filters.eq;

public class StockItemCreatedHandler implements IMessageHandler<StockItemCreated> {
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
