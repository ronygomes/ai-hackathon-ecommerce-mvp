package me.ronygomes.ecommerce.inventory.presentation.eventhandler.handler;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import me.ronygomes.ecommerce.inventory.domain.StockSet;
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

class StockSetHandlerTest {

    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> collection = mock(MongoCollection.class);
    private StockSetHandler handler;

    @BeforeEach
    void setUp() {
        handler = new StockSetHandler(collection);
    }

    @Test
    void handle_upsertsNewQuantityAndFlagsInStockWhenPositive() throws Exception {
        UUID pid = UUID.randomUUID();

        handler.handle(new StockSet(pid, 0, 12, "restock", "admin")).get();

        ArgumentCaptor<Document> doc = ArgumentCaptor.forClass(Document.class);
        verify(collection).replaceOne(any(Bson.class), doc.capture(), any(ReplaceOptions.class));
        assertThat(doc.getValue())
                .containsEntry("_id", pid.toString())
                .containsEntry("availableQty", 12)
                .containsEntry("inStockFlag", true);
    }

    @Test
    void handle_setToZero_flagsInStockFalse() throws Exception {
        UUID pid = UUID.randomUUID();

        handler.handle(new StockSet(pid, 5, 0, "out", "admin")).get();

        ArgumentCaptor<Document> doc = ArgumentCaptor.forClass(Document.class);
        verify(collection).replaceOne(any(Bson.class), doc.capture(), any(ReplaceOptions.class));
        assertThat(doc.getValue()).containsEntry("inStockFlag", false);
    }

    @Test
    void getMessageType_returnsStockSet() {
        assertThat(handler.getMessageType()).isEqualTo(StockSet.class);
    }
}
