package me.ronygomes.ecommerce.ordering.presentation.eventhandler.handler;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import me.ronygomes.ecommerce.checkout.saga.message.event.OrderCreated;
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

class OrderCreatedProjectionHandlerTest {

    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> collection = mock(MongoCollection.class);
    private OrderCreatedProjectionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OrderCreatedProjectionHandler(collection);
    }

    @Test
    void handle_upsertsOrderProjectionWithCompletedStatus() throws Exception {
        String orderId = UUID.randomUUID().toString();

        handler.handle(new OrderCreated(orderId, "g1", "jane@example.com")).get();

        ArgumentCaptor<Document> doc = ArgumentCaptor.forClass(Document.class);
        verify(collection).replaceOne(any(Bson.class), doc.capture(), any(ReplaceOptions.class));
        assertThat(doc.getValue())
                .containsEntry("_id", orderId)
                .containsEntry("guestToken", "g1")
                .containsEntry("status", "CONFIRMED")
                .containsEntry("customerEmail", "jane@example.com");
    }

    @Test
    void getMessageType_returnsOrderCreated() {
        assertThat(handler.getMessageType()).isEqualTo(OrderCreated.class);
    }
}
