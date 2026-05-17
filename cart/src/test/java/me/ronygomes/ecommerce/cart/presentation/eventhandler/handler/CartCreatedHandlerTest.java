package me.ronygomes.ecommerce.cart.presentation.eventhandler.handler;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import me.ronygomes.ecommerce.cart.domain.CartCreated;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CartCreatedHandlerTest {

    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> collection = mock(MongoCollection.class);
    private CartCreatedHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CartCreatedHandler(collection);
    }

    @Test
    void handle_upsertsInitialCartDocumentWithEmptyItems() throws Exception {
        UUID cartId = UUID.randomUUID();

        handler.handle(new CartCreated(cartId, "guest-1")).get();

        ArgumentCaptor<Document> doc = ArgumentCaptor.forClass(Document.class);
        verify(collection).replaceOne(any(Bson.class), doc.capture(), any(ReplaceOptions.class));
        assertThat(doc.getValue())
                .containsEntry("_id", cartId.toString())
                .containsEntry("cartId", cartId.toString())
                .containsEntry("guestToken", "guest-1")
                .containsEntry("items", List.of());
    }

    @Test
    void getMessageType_returnsCartCreated() {
        assertThat(handler.getMessageType()).isEqualTo(CartCreated.class);
    }
}
