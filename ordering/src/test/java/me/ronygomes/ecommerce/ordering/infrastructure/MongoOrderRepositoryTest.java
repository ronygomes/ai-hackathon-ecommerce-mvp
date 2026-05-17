package me.ronygomes.ecommerce.ordering.infrastructure;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import me.ronygomes.ecommerce.ordering.domain.IdempotencyKey;
import me.ronygomes.ecommerce.ordering.domain.Order;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MongoOrderRepositoryTest {

    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> collection = mock(MongoCollection.class);
    @SuppressWarnings("unchecked")
    private final FindIterable<Document> iterable = mock(FindIterable.class);
    private MongoOrderRepository repository;

    @BeforeEach
    void setUp() {
        MongoClient client = mock(MongoClient.class);
        MongoDatabase database = mock(MongoDatabase.class);
        when(client.getDatabase("aihackathon")).thenReturn(database);
        when(database.getCollection("orders")).thenReturn(collection);
        when(collection.find(any(Bson.class))).thenReturn(iterable);

        repository = new MongoOrderRepository(client);
    }

    @Test
    void getByIdempotencyKey_whenNotFound_returnsEmpty() throws Exception {
        when(iterable.first()).thenReturn(null);

        Optional<Order> result = repository.getByIdempotencyKey(new IdempotencyKey(UUID.randomUUID())).get();

        assertThat(result).isEmpty();
    }

    @Test
    void getByIdempotencyKey_whenDocFound_currentlyFailsToDeserialize() {
        // Pins a real bug: Order has only a private all-args constructor and no @JsonCreator,
        // so Jackson cannot reconstruct it from BSON. Logged in CLAUDE.md.
        Document doc = new Document("_id", "x")
                .append("idempotencyKey", new Document("value", UUID.randomUUID().toString()));
        when(iterable.first()).thenReturn(doc);

        assertThatThrownBy(() -> repository.getByIdempotencyKey(new IdempotencyKey(UUID.randomUUID())).get())
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("Failed to deserialize order");
    }
}
