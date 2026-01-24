package com.ecommerce.productcatalog.processes.eventhandler.handlers;

import com.ecommerce.core.messaging.IMessageHandler;
import com.ecommerce.productcatalog.domain.ProductDeactivated;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import java.util.concurrent.CompletableFuture;
import static com.mongodb.client.model.Filters.eq;

public class ProductDeactivatedProjectionHandler implements IMessageHandler<ProductDeactivated> {
    private final MongoCollection<Document> listView;
    private final MongoCollection<Document> detailView;

    public ProductDeactivatedProjectionHandler(MongoCollection<Document> listView,
            MongoCollection<Document> detailView) {
        this.listView = listView;
        this.detailView = detailView;
    }

    @Override
    public CompletableFuture<Void> handle(ProductDeactivated event) {
        String pid = event.productId().toString();

        listView.updateOne(eq("_id", pid), Updates.set("isActive", false));
        detailView.updateOne(eq("_id", pid), Updates.set("isActive", false));

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Class<ProductDeactivated> getMessageType() {
        return ProductDeactivated.class;
    }
}
