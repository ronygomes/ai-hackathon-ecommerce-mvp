package me.ronygomes.ecommerce.cart.infrastructure;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import me.ronygomes.ecommerce.cart.domain.GuestToken;
import me.ronygomes.ecommerce.cart.domain.ShoppingCart;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MongoCartRepositoryTest {

    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> collection = mock(MongoCollection.class);
    @SuppressWarnings("unchecked")
    private final FindIterable<Document> iterable = mock(FindIterable.class);
    private MongoCartRepository repository;

    @BeforeEach
    void setUp() {
        MongoClient client = mock(MongoClient.class);
        MongoDatabase database = mock(MongoDatabase.class);
        when(client.getDatabase("aihackathon")).thenReturn(database);
        when(database.getCollection("carts")).thenReturn(collection);
        when(collection.find(any(Bson.class))).thenReturn(iterable);

        repository = new MongoCartRepository(client);
    }

    @Test
    void getByGuestToken_whenNotFound_returnsEmpty() throws Exception {
        when(iterable.first()).thenReturn(null);

        Optional<ShoppingCart> result = repository.getByGuestToken(new GuestToken("g1")).get();

        assertThat(result).isEmpty();
    }

    @Test
    void getByGuestToken_whenDocFound_currentlyFailsToDeserialize() {
        // Pins a real bug: ShoppingCart has only a private (CartId, GuestToken) constructor
        // and no @JsonCreator, so Jackson cannot reconstruct it from BSON. Logged in CLAUDE.md.
        Document doc = new Document("_id", "x")
                .append("guestToken", new Document("value", "g1"));
        when(iterable.first()).thenReturn(doc);

        assertThatThrownBy(() -> repository.getByGuestToken(new GuestToken("g1")).get())
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("Failed to deserialize cart");
    }
}
