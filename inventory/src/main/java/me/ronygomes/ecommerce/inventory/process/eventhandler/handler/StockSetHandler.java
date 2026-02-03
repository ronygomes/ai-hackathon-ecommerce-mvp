package me.ronygomes.ecommerce.inventory.process.eventhandler.handler;

import me.ronygomes.ecommerce.core.messaging.MessageHandler;
import me.ronygomes.ecommerce.inventory.domain.StockSet;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import java.util.concurrent.CompletableFuture;
import static com.mongodb.client.model.Filters.eq;

public class StockSetHandler implements MessageHandler<StockSet> {
    private final MongoCollection<Document> collection;

    public StockSetHandler(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    @Override
    public CompletableFuture<Void> handle(StockSet event) {
        String pid = event.productId().toString();
        Document stockDoc = new Document()
                .append("_id", pid)
                .append("productId", pid)
                .append("availableQty", event.newQty())
                .append("inStockFlag", event.newQty() > 0);

        collection.replaceOne(eq("_id", pid), stockDoc, new ReplaceOptions().upsert(true));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Class<StockSet> getMessageType() {
        return StockSet.class;
    }
}
