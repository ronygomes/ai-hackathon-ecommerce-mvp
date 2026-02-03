package me.ronygomes.ecommerce.productcatalog.process.eventhandler.handler;

import me.ronygomes.ecommerce.core.messaging.MessageHandler;
import me.ronygomes.ecommerce.productcatalog.domain.ProductActivated;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import java.util.concurrent.CompletableFuture;
import static com.mongodb.client.model.Filters.eq;

public class ProductActivatedProjectionHandler implements MessageHandler<ProductActivated> {
    private final MongoCollection<Document> listView;
    private final MongoCollection<Document> detailView;

    public ProductActivatedProjectionHandler(MongoCollection<Document> listView, MongoCollection<Document> detailView) {
        this.listView = listView;
        this.detailView = detailView;
    }

    @Override
    public CompletableFuture<Void> handle(ProductActivated event) {
        String pid = event.productId().toString();

        listView.updateOne(eq("_id", pid), Updates.set("isActive", true));
        detailView.updateOne(eq("_id", pid), Updates.set("isActive", true));

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Class<ProductActivated> getMessageType() {
        return ProductActivated.class;
    }
}
