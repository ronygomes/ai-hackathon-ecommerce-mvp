package me.ronygomes.ecommerce.core.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import me.ronygomes.ecommerce.core.domain.DomainEvent;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MongoOutboxStoreTest {

    public record SampleEvent(String getEventId, long getTimestamp, String payload) implements DomainEvent {
    }

    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> collection = mock(MongoCollection.class);
    private MongoOutboxStore store;

    @BeforeEach
    void setUp() {
        store = new MongoOutboxStore(collection, new ObjectMapper());
    }

    @Test
    void append_insertsOneDocumentPerEvent() {
        store.append("agg-1", List.of(
                new SampleEvent("e1", 1L, "hello"),
                new SampleEvent("e2", 2L, "world")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> docsCaptor = ArgumentCaptor.forClass(List.class);
        verify(collection).insertMany(docsCaptor.capture());

        List<Document> docs = docsCaptor.getValue();
        assertThat(docs).hasSize(2);
        assertThat(docs).allSatisfy(doc -> {
            assertThat(doc.getString("_id")).isNotBlank();
            assertThat(doc.getString("aggregateId")).isEqualTo("agg-1");
            assertThat(doc.getString("eventType")).isEqualTo("SampleEvent");
            assertThat(doc.getString("payload")).contains("\"payload\":");
            assertThat(doc.getLong("createdAt")).isPositive();
            assertThat(doc.get("publishedAt")).isNull();
        });
    }

    @Test
    void append_emptyList_doesNothing() {
        store.append("agg-1", List.of());

        verify(collection, never()).insertMany(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findPending_returnsDeserializedEntriesInCreatedOrder() {
        FindIterable<Document> iterable = mock(FindIterable.class);
        MongoCursor<Document> cursor = mock(MongoCursor.class);
        when(collection.find(any(Bson.class))).thenReturn(iterable);
        when(iterable.sort(any())).thenReturn(iterable);
        when(iterable.limit(50)).thenReturn(iterable);
        when(iterable.iterator()).thenReturn(cursor);

        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();
        Document a = new Document("_id", id1).append("aggregateId", "agg-1")
                .append("eventType", "SampleEvent").append("payload", "{\"x\":1}")
                .append("createdAt", 100L).append("publishedAt", null);
        Document b = new Document("_id", id2).append("aggregateId", "agg-2")
                .append("eventType", "SampleEvent").append("payload", "{\"y\":2}")
                .append("createdAt", 200L).append("publishedAt", null);
        when(cursor.hasNext()).thenReturn(true, true, false);
        when(cursor.next()).thenReturn(a, b);

        List<OutboxEntry> result = store.findPending(50);

        assertThat(result).extracting(OutboxEntry::id).containsExactly(id1, id2);
        assertThat(result).allMatch(OutboxEntry::isPending);
    }

    @Test
    void markPublished_updatesEveryMatchingDocWithTimestamp() {
        store.markPublished(List.of("id-1", "id-2"));

        verify(collection).updateMany(any(Bson.class), any(Bson.class));
    }

    @Test
    void markPublished_emptyList_doesNothing() {
        store.markPublished(List.of());

        verify(collection, never()).updateMany(any(Bson.class), any(Bson.class));
    }
}
