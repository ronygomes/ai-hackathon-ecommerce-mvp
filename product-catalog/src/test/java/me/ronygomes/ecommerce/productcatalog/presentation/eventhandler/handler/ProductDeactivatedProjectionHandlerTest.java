package me.ronygomes.ecommerce.productcatalog.presentation.eventhandler.handler;

import com.mongodb.client.MongoCollection;
import me.ronygomes.ecommerce.productcatalog.domain.ProductDeactivated;
import me.ronygomes.ecommerce.productcatalog.domain.ProductId;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProductDeactivatedProjectionHandlerTest {

    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> listView = mock(MongoCollection.class);
    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> detailView = mock(MongoCollection.class);
    private ProductDeactivatedProjectionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ProductDeactivatedProjectionHandler(listView, detailView);
    }

    @Test
    void handle_setsIsActiveFalseOnBothViews() throws Exception {
        ProductDeactivated event = new ProductDeactivated(new ProductId(UUID.randomUUID()));

        handler.handle(event).get();

        verify(listView).updateOne(any(Bson.class), any(Bson.class));
        verify(detailView).updateOne(any(Bson.class), any(Bson.class));
    }

    @Test
    void getMessageType_returnsProductDeactivatedClass() {
        assertThat(handler.getMessageType()).isEqualTo(ProductDeactivated.class);
    }
}
