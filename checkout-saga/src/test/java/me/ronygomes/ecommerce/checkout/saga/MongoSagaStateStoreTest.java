package me.ronygomes.ecommerce.checkout.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MongoSagaStateStoreTest {

    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> collection = mock(MongoCollection.class);
    @SuppressWarnings("unchecked")
    private final FindIterable<Document> findOneIterable = mock(FindIterable.class);
    private MongoSagaStateStore store;

    @BeforeEach
    void setUp() {
        when(collection.find(any(Bson.class))).thenReturn(findOneIterable);
        store = new MongoSagaStateStore(collection, new ObjectMapper());
    }

    @Test
    void findByOrderId_whenNotFound_returnsEmpty() {
        when(findOneIterable.first()).thenReturn(null);

        assertThat(store.findByOrderId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void findByOrderId_whenFound_returnsDeserializedState() {
        UUID orderId = UUID.randomUUID();
        Document doc = Document.parse("{\"orderId\":\"" + orderId + "\",\"guestToken\":\"g1\",\"idempotencyKey\":\"k1\"}");
        doc.put("_id", orderId.toString());
        when(findOneIterable.first()).thenReturn(doc);

        Optional<SagaState> result = store.findByOrderId(orderId);

        assertThat(result).hasValueSatisfying(state -> {
            assertThat(state.orderId).isEqualTo(orderId);
            assertThat(state.guestToken).isEqualTo("g1");
            assertThat(state.idempotencyKey).isEqualTo("k1");
        });
    }

    @Test
    void findByOrderId_whenDocumentMalformed_throws() {
        Document doc = new Document("_id", "x").append("guestToken", new Object());
        when(findOneIterable.first()).thenReturn(doc);

        assertThatThrownBy(() -> store.findByOrderId(UUID.randomUUID()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to deserialize saga state");
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_returnsEveryDeserializedDocument() {
        FindIterable<Document> all = mock(FindIterable.class);
        MongoCursor<Document> cursor = mock(MongoCursor.class);
        UUID orderA = UUID.randomUUID();
        UUID orderB = UUID.randomUUID();
        Document a = Document.parse("{\"orderId\":\"" + orderA + "\",\"guestToken\":\"ga\",\"idempotencyKey\":\"ka\"}");
        a.put("_id", orderA.toString());
        Document b = Document.parse("{\"orderId\":\"" + orderB + "\",\"guestToken\":\"gb\",\"idempotencyKey\":\"kb\"}");
        b.put("_id", orderB.toString());
        when(collection.find()).thenReturn(all);
        when(all.iterator()).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, true, false);
        when(cursor.next()).thenReturn(a, b);

        Collection<SagaState> result = store.findAll();

        assertThat(result).extracting(s -> s.guestToken).containsExactlyInAnyOrder("ga", "gb");
    }

    @Test
    void save_serializesAndUpsertsKeyedByOrderId() {
        UUID orderId = UUID.randomUUID();
        SagaState state = new SagaState(orderId, "g1", "k1");
        state.totalItemsToDeduct = 4;

        store.save(state);

        ArgumentCaptor<Document> doc = ArgumentCaptor.forClass(Document.class);
        ArgumentCaptor<ReplaceOptions> opts = ArgumentCaptor.forClass(ReplaceOptions.class);
        verify(collection).replaceOne(any(Bson.class), doc.capture(), opts.capture());
        assertThat(doc.getValue().getString("_id")).isEqualTo(orderId.toString());
        assertThat(doc.getValue().getString("guestToken")).isEqualTo("g1");
        assertThat(doc.getValue().getInteger("totalItemsToDeduct")).isEqualTo(4);
        assertThat(opts.getValue().isUpsert()).isTrue();
    }

    @Test
    void remove_deletesByOrderId() {
        UUID orderId = UUID.randomUUID();

        store.remove(orderId);

        verify(collection).deleteOne(any(Bson.class));
    }
}
