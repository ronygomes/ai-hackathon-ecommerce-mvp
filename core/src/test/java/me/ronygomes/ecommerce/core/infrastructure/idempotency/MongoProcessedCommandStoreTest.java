package me.ronygomes.ecommerce.core.infrastructure.idempotency;

import com.mongodb.MongoWriteException;
import com.mongodb.ServerAddress;
import com.mongodb.WriteError;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MongoProcessedCommandStoreTest {

    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> collection = mock(MongoCollection.class);
    @SuppressWarnings("unchecked")
    private final FindIterable<Document> iterable = mock(FindIterable.class);
    private MongoProcessedCommandStore store;

    @BeforeEach
    void setUp() {
        when(collection.find(any(Bson.class))).thenReturn(iterable);
        store = new MongoProcessedCommandStore(collection);
    }

    @Test
    void wasProcessed_whenNoDoc_returnsFalse() {
        when(iterable.first()).thenReturn(null);

        assertThat(store.wasProcessed("cmd-1")).isFalse();
    }

    @Test
    void wasProcessed_whenDocExists_returnsTrue() {
        when(iterable.first()).thenReturn(new Document("_id", "cmd-1"));

        assertThat(store.wasProcessed("cmd-1")).isTrue();
    }

    @Test
    void markProcessed_insertsDocumentKeyedByCommandId() {
        store.markProcessed("cmd-1", "handler=Create");

        ArgumentCaptor<Document> doc = ArgumentCaptor.forClass(Document.class);
        verify(collection).insertOne(doc.capture());
        assertThat(doc.getValue().getString("_id")).isEqualTo("cmd-1");
        assertThat(doc.getValue().getString("metadata")).isEqualTo("handler=Create");
        assertThat(doc.getValue().getLong("processedAt")).isPositive();
    }

    @Test
    void markProcessed_duplicateKey_isSwallowedAsIdempotentNoOp() {
        // Mongo error code 11000 is the canonical duplicate-key error.
        MongoWriteException duplicate = new MongoWriteException(
                new WriteError(11000, "dup", new BsonDocument()),
                new ServerAddress(),
                java.util.Set.of());
        doThrow(duplicate).when(collection).insertOne(any(Document.class));

        store.markProcessed("cmd-1", "x");

        verify(collection).insertOne(any(Document.class));
    }

    @Test
    void markProcessed_nonDuplicateMongoError_propagates() {
        MongoWriteException other = new MongoWriteException(
                new WriteError(123, "other", new BsonDocument()),
                new ServerAddress(),
                java.util.Set.of());
        doThrow(other).when(collection).insertOne(any(Document.class));

        assertThatThrownBy(() -> store.markProcessed("cmd-1", "x"))
                .isInstanceOf(MongoWriteException.class);
    }
}
