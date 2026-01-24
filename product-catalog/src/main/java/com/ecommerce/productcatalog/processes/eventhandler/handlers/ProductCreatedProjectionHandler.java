package com.ecommerce.productcatalog.processes.eventhandler.handlers;

import com.ecommerce.core.messaging.IMessageHandler;
import com.ecommerce.productcatalog.domain.ProductCreated;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import java.util.concurrent.CompletableFuture;
import static com.mongodb.client.model.Filters.eq;

public class ProductCreatedProjectionHandler implements IMessageHandler<ProductCreated> {
    private final MongoCollection<Document> listView;
    private final MongoCollection<Document> detailView;

    public ProductCreatedProjectionHandler(MongoCollection<Document> listView, MongoCollection<Document> detailView) {
        this.listView = listView;
        this.detailView = detailView;
    }

    @Override
    public CompletableFuture<Void> handle(ProductCreated event) {
        String pid = event.productId().toString();

        Document listDoc = new Document()
                .append("_id", pid)
                .append("productId", pid)
                .append("name", event.name().value())
                .append("price", event.price().value())
                .append("isActive", false);
        listView.replaceOne(eq("_id", pid), listDoc, new ReplaceOptions().upsert(true));

        Document detailDoc = new Document()
                .append("_id", pid)
                .append("sku", event.sku().value())
                .append("name", event.name().value())
                .append("description", event.description().value())
                .append("price", event.price().value())
                .append("isActive", false);
        detailView.replaceOne(eq("_id", pid), detailDoc, new ReplaceOptions().upsert(true));

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Class<ProductCreated> getMessageType() {
        return ProductCreated.class;
    }
}
