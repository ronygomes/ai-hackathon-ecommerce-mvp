package me.ronygomes.ecommerce.productcatalog.presentation.eventhandler.handler;

import com.mongodb.client.MongoCollection;
import me.ronygomes.ecommerce.productcatalog.domain.ProductDescription;
import me.ronygomes.ecommerce.productcatalog.domain.ProductDetailsUpdated;
import me.ronygomes.ecommerce.productcatalog.domain.ProductId;
import me.ronygomes.ecommerce.productcatalog.domain.ProductName;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProductDetailsUpdatedProjectionHandlerTest {

    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> listView = mock(MongoCollection.class);
    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> detailView = mock(MongoCollection.class);
    private ProductDetailsUpdatedProjectionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ProductDetailsUpdatedProjectionHandler(listView, detailView);
    }

    @Test
    void handle_updatesNameOnListViewAndNameDescriptionOnDetailView() throws Exception {
        ProductId id = new ProductId(UUID.randomUUID());
        ProductDetailsUpdated event = new ProductDetailsUpdated(id, new ProductName("New"), new ProductDescription("Desc"));

        handler.handle(event).get();

        verify(listView).updateOne(any(Bson.class), any(Bson.class));
        verify(detailView).updateOne(any(Bson.class), any(Bson.class));
    }

    @Test
    void getMessageType_returnsProductDetailsUpdatedClass() {
        assertThat(handler.getMessageType()).isEqualTo(ProductDetailsUpdated.class);
    }
}
