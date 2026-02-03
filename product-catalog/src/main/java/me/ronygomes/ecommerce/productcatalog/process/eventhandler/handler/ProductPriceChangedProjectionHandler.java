package me.ronygomes.ecommerce.productcatalog.process.eventhandler.handler;

import me.ronygomes.ecommerce.core.messaging.MessageHandler;
import me.ronygomes.ecommerce.productcatalog.domain.ProductPriceChanged;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import java.util.concurrent.CompletableFuture;
import static com.mongodb.client.model.Filters.eq;

public class ProductPriceChangedProjectionHandler implements MessageHandler<ProductPriceChanged> {
    private final MongoCollection<Document> listView;
    private final MongoCollection<Document> detailView;

    public ProductPriceChangedProjectionHandler(MongoCollection<Document> listView,
            MongoCollection<Document> detailView) {
        this.listView = listView;
        this.detailView = detailView;
    }

    @Override
    public CompletableFuture<Void> handle(ProductPriceChanged event) {
        String pid = event.productId().toString();
        double newPrice = event.newPrice().value();

        listView.updateOne(eq("_id", pid), Updates.set("price", newPrice));
        detailView.updateOne(eq("_id", pid), Updates.set("price", newPrice));

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Class<ProductPriceChanged> getMessageType() {
        return ProductPriceChanged.class;
    }
}
