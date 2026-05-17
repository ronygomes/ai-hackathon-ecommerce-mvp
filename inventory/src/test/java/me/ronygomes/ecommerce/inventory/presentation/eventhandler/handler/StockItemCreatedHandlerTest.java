package me.ronygomes.ecommerce.inventory.presentation.eventhandler.handler;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import me.ronygomes.ecommerce.inventory.domain.StockItemCreated;
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

class StockItemCreatedHandlerTest {

    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> collection = mock(MongoCollection.class);
    private StockItemCreatedHandler handler;

    @BeforeEach
    void setUp() {
        handler = new StockItemCreatedHandler(collection);
    }

    @Test
    void handle_upsertsDocumentWithInStockFlagTrueWhenQtyPositive() throws Exception {
        UUID pid = UUID.randomUUID();

        handler.handle(new StockItemCreated(pid, 5)).get();

        ArgumentCaptor<Document> doc = ArgumentCaptor.forClass(Document.class);
        verify(collection).replaceOne(any(Bson.class), doc.capture(), any(ReplaceOptions.class));
        assertThat(doc.getValue())
                .containsEntry("_id", pid.toString())
                .containsEntry("productId", pid.toString())
                .containsEntry("availableQty", 5)
                .containsEntry("inStockFlag", true);
    }

    @Test
    void handle_zeroQuantity_marksInStockFlagFalse() throws Exception {
        UUID pid = UUID.randomUUID();

        handler.handle(new StockItemCreated(pid, 0)).get();

        ArgumentCaptor<Document> doc = ArgumentCaptor.forClass(Document.class);
        verify(collection).replaceOne(any(Bson.class), doc.capture(), any(ReplaceOptions.class));
        assertThat(doc.getValue()).containsEntry("inStockFlag", false);
    }

    @Test
    void getMessageType_returnsStockItemCreated() {
        assertThat(handler.getMessageType()).isEqualTo(StockItemCreated.class);
    }
}
