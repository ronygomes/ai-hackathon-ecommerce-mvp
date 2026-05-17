package me.ronygomes.ecommerce.cart.presentation.eventhandler.handler;

import com.mongodb.client.MongoCollection;
import me.ronygomes.ecommerce.cart.domain.CartItemAdded;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CartItemAddedHandlerTest {

    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> collection = mock(MongoCollection.class);
    private CartItemAddedHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CartItemAddedHandler(collection);
    }

    @Test
    void handle_pushesItemDocumentOntoCartItemsArray() throws Exception {
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        handler.handle(new CartItemAdded(cartId, productId, 4)).get();

        verify(collection).updateOne(any(Bson.class), any(Bson.class));
    }

    @Test
    void getMessageType_returnsCartItemAdded() {
        assertThat(handler.getMessageType()).isEqualTo(CartItemAdded.class);
    }
}
