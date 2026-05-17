package me.ronygomes.ecommerce.productcatalog.presentation.eventhandler.handler;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import me.ronygomes.ecommerce.productcatalog.domain.Price;
import me.ronygomes.ecommerce.productcatalog.domain.ProductCreated;
import me.ronygomes.ecommerce.productcatalog.domain.ProductDescription;
import me.ronygomes.ecommerce.productcatalog.domain.ProductId;
import me.ronygomes.ecommerce.productcatalog.domain.ProductName;
import me.ronygomes.ecommerce.productcatalog.domain.Sku;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProductCreatedProjectionHandlerTest {

    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> listView = mock(MongoCollection.class);
    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> detailView = mock(MongoCollection.class);
    private ProductCreatedProjectionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ProductCreatedProjectionHandler(listView, detailView);
    }

    @Test
    void handle_upsertsListAndDetailDocumentsWithDefaultIsActiveFalse() throws Exception {
        ProductId id = new ProductId(UUID.randomUUID());
        ProductCreated event = new ProductCreated(id, new Sku("SKU"), new ProductName("Widget"),
                new Price(9.99), new ProductDescription("desc"));

        handler.handle(event).get();

        ArgumentCaptor<Document> listDoc = ArgumentCaptor.forClass(Document.class);
        ArgumentCaptor<Document> detailDoc = ArgumentCaptor.forClass(Document.class);
        verify(listView).replaceOne(any(Bson.class), listDoc.capture(), any(ReplaceOptions.class));
        verify(detailView).replaceOne(any(Bson.class), detailDoc.capture(), any(ReplaceOptions.class));

        assertThat(listDoc.getValue())
                .containsEntry("_id", id.toString())
                .containsEntry("name", "Widget")
                .containsEntry("price", 9.99)
                .containsEntry("isActive", false);

        assertThat(detailDoc.getValue())
                .containsEntry("_id", id.toString())
                .containsEntry("sku", "SKU")
                .containsEntry("description", "desc")
                .containsEntry("isActive", false);
    }

    @Test
    void getMessageType_returnsProductCreatedClass() {
        assertThat(handler.getMessageType()).isEqualTo(ProductCreated.class);
    }
}
