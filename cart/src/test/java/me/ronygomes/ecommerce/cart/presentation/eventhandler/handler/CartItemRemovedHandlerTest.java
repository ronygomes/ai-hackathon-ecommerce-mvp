package me.ronygomes.ecommerce.cart.presentation.eventhandler.handler;

import com.mongodb.client.MongoCollection;
import me.ronygomes.ecommerce.cart.domain.CartItemRemoved;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CartItemRemovedHandlerTest {

    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> collection = mock(MongoCollection.class);
    private CartItemRemovedHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CartItemRemovedHandler(collection);
    }

    @Test
    void handle_pullsMatchingItemFromItemsArray() throws Exception {
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        handler.handle(new CartItemRemoved(cartId, productId)).get();

        verify(collection).updateOne(any(Bson.class), any(Bson.class));
    }

    @Test
    void getMessageType_returnsCartItemRemoved() {
        assertThat(handler.getMessageType()).isEqualTo(CartItemRemoved.class);
    }
}
