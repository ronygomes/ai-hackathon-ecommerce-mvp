package me.ronygomes.ecommerce.cart.presentation.eventhandler.handler;

import com.mongodb.client.MongoCollection;
import me.ronygomes.ecommerce.cart.domain.CartItemQuantityUpdated;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CartItemQuantityUpdatedHandlerTest {

    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> collection = mock(MongoCollection.class);
    private CartItemQuantityUpdatedHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CartItemQuantityUpdatedHandler(collection);
    }

    @Test
    void handle_updatesMatchingItemQuantityViaPositionalOperator() throws Exception {
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        handler.handle(new CartItemQuantityUpdated(cartId, productId, 2, 7)).get();

        verify(collection).updateOne(any(Bson.class), any(Bson.class));
    }

    @Test
    void getMessageType_returnsCartItemQuantityUpdated() {
        assertThat(handler.getMessageType()).isEqualTo(CartItemQuantityUpdated.class);
    }
}
