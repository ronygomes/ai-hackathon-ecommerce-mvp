package com.ecommerce.productcatalog.processes.eventhandler.handlers;

import com.ecommerce.core.messaging.IMessageHandler;
import com.ecommerce.productcatalog.domain.ProductDetailsUpdated;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import java.util.concurrent.CompletableFuture;
import static com.mongodb.client.model.Filters.eq;

public class ProductDetailsUpdatedProjectionHandler implements IMessageHandler<ProductDetailsUpdated> {
    private final MongoCollection<Document> listView;
    private final MongoCollection<Document> detailView;

    public ProductDetailsUpdatedProjectionHandler(MongoCollection<Document> listView,
            MongoCollection<Document> detailView) {
        this.listView = listView;
        this.detailView = detailView;
    }

    @Override
    public CompletableFuture<Void> handle(ProductDetailsUpdated event) {
        String pid = event.productId().toString();

        listView.updateOne(eq("_id", pid), Updates.set("name", event.name().value()));

        Bson detailUpdates = Updates.combine(
                Updates.set("name", event.name().value()),
                Updates.set("description", event.description().value()));
        detailView.updateOne(eq("_id", pid), detailUpdates);

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Class<ProductDetailsUpdated> getMessageType() {
        return ProductDetailsUpdated.class;
    }
}
