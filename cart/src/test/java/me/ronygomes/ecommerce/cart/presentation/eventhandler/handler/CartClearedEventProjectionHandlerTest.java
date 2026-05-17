package me.ronygomes.ecommerce.cart.presentation.eventhandler.handler;

import com.mongodb.client.MongoCollection;
import me.ronygomes.ecommerce.checkout.saga.message.event.CartCleared;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CartClearedEventProjectionHandlerTest {

    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> collection = mock(MongoCollection.class);
    private CartClearedEventProjectionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CartClearedEventProjectionHandler(collection);
    }

    @Test
    void handle_clearsItemsArrayOnCartProjection() throws Exception {
        handler.handle(new CartCleared("guest-1")).get();

        verify(collection).updateOne(any(Bson.class), any(Bson.class));
    }

    @Test
    void getMessageType_returnsCartCleared() {
        assertThat(handler.getMessageType()).isEqualTo(CartCleared.class);
    }
}
