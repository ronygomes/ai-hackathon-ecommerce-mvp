package me.ronygomes.ecommerce.productcatalog.presentation.eventhandler.handler;

import com.mongodb.client.MongoCollection;
import me.ronygomes.ecommerce.productcatalog.domain.Price;
import me.ronygomes.ecommerce.productcatalog.domain.ProductId;
import me.ronygomes.ecommerce.productcatalog.domain.ProductPriceChanged;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProductPriceChangedProjectionHandlerTest {

    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> listView = mock(MongoCollection.class);
    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> detailView = mock(MongoCollection.class);
    private ProductPriceChangedProjectionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ProductPriceChangedProjectionHandler(listView, detailView);
    }

    @Test
    void handle_updatesPriceOnBothViews() throws Exception {
        ProductId id = new ProductId(UUID.randomUUID());
        ProductPriceChanged event = new ProductPriceChanged(id, new Price(1.0), new Price(2.0));

        handler.handle(event).get();

        verify(listView).updateOne(any(Bson.class), any(Bson.class));
        verify(detailView).updateOne(any(Bson.class), any(Bson.class));
    }

    @Test
    void getMessageType_returnsProductPriceChangedClass() {
        assertThat(handler.getMessageType()).isEqualTo(ProductPriceChanged.class);
    }
}
