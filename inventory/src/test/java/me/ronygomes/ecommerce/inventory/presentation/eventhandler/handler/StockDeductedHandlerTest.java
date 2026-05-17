package me.ronygomes.ecommerce.inventory.presentation.eventhandler.handler;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import me.ronygomes.ecommerce.checkout.saga.message.event.StockDeductedForOrder;
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

class StockDeductedHandlerTest {

    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> collection = mock(MongoCollection.class);
    private StockDeductedHandler handler;

    @BeforeEach
    void setUp() {
        handler = new StockDeductedHandler(collection);
    }

    @Test
    void handle_upsertsRemainingQtyAndKeepsInStockFlagTrueWhenPositive() throws Exception {
        UUID pid = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        handler.handle(new StockDeductedForOrder(orderId, pid, 6)).get();

        ArgumentCaptor<Document> doc = ArgumentCaptor.forClass(Document.class);
        verify(collection).replaceOne(any(Bson.class), doc.capture(), any(ReplaceOptions.class));
        assertThat(doc.getValue())
                .containsEntry("_id", pid.toString())
                .containsEntry("availableQty", 6)
                .containsEntry("inStockFlag", true);
    }

    @Test
    void handle_drainedToZero_flagsInStockFalse() throws Exception {
        UUID pid = UUID.randomUUID();

        handler.handle(new StockDeductedForOrder(UUID.randomUUID(), pid, 0)).get();

        ArgumentCaptor<Document> doc = ArgumentCaptor.forClass(Document.class);
        verify(collection).replaceOne(any(Bson.class), doc.capture(), any(ReplaceOptions.class));
        assertThat(doc.getValue()).containsEntry("inStockFlag", false);
    }

    @Test
    void getMessageType_returnsStockDeductedForOrder() {
        assertThat(handler.getMessageType()).isEqualTo(StockDeductedForOrder.class);
    }
}
