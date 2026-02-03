package me.ronygomes.ecommerce.inventory.presentation.eventhandler.handler;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import me.rongyomes.ecommerce.checkout.saga.message.event.StockDeductedForOrder;
import me.ronygomes.ecommerce.core.messaging.MessageHandler;
import org.bson.Document;

import java.util.concurrent.CompletableFuture;

import static com.mongodb.client.model.Filters.eq;

public class StockDeductedHandler implements MessageHandler<StockDeductedForOrder> {
    private final MongoCollection<Document> collection;

    public StockDeductedHandler(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    @Override
    public CompletableFuture<Void> handle(StockDeductedForOrder event) {
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
    public Class<StockDeductedForOrder> getMessageType() {
        return StockDeductedForOrder.class;
    }
}
