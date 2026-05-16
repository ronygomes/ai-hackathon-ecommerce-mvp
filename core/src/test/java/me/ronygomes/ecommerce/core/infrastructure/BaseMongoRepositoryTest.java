package me.ronygomes.ecommerce.core.infrastructure;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import me.ronygomes.ecommerce.core.domain.BaseAggregate;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BaseMongoRepositoryTest {

    public static class TestAggregate extends BaseAggregate<String> {
        private String name;

        public TestAggregate() {
        }

        public TestAggregate(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    private static final class TestRepository extends BaseMongoRepository<TestAggregate, String> {
        TestRepository(MongoClient mongoClient) {
            super(mongoClient, "test_db", "test_coll", TestAggregate.class);
        }
    }

    private MongoCollection<Document> collection;
    private FindIterable<Document> findIterable;
    private TestRepository repository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        MongoClient client = mock(MongoClient.class);
        MongoDatabase database = mock(MongoDatabase.class);
        collection = mock(MongoCollection.class);
        findIterable = mock(FindIterable.class);

        when(client.getDatabase("test_db")).thenReturn(database);
        when(database.getCollection("test_coll")).thenReturn(collection);
        when(collection.find(any(Bson.class))).thenReturn(findIterable);

        repository = new TestRepository(client);
    }

    @Test
    void getById_whenDocumentExists_returnsDeserializedAggregate() throws Exception {
        // NOTE: Mongo's `_id` is stripped before deserialization (see BaseMongoRepository.getById).
        // BaseAggregate.id has no public setter, so id stays null on load — known bug, tracked in
        // ai_spec/CLAUDE.md. Test asserts the parts that actually round-trip today.
        Document doc = new Document("_id", "id-1").append("name", "hello");
        when(findIterable.first()).thenReturn(doc);

        Optional<TestAggregate> result = repository.getById("id-1").get();

        assertThat(result).hasValueSatisfying(agg -> assertThat(agg.getName()).isEqualTo("hello"));
    }

    @Test
    void getById_whenNotFound_returnsEmpty() throws Exception {
        when(findIterable.first()).thenReturn(null);

        Optional<TestAggregate> result = repository.getById("missing").get();

        assertThat(result).isEmpty();
    }

    @Test
    void getById_whenDocumentMalformed_completesExceptionally() {
        Document doc = new Document("_id", "id-1").append("name", new Object());
        when(findIterable.first()).thenReturn(doc);

        assertThatThrownBy(() -> repository.getById("id-1").get())
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("Failed to deserialize aggregate");
    }

    @Test
    void save_upsertsDocumentKeyedByAggregateId() throws Exception {
        TestAggregate aggregate = new TestAggregate("id-1", "hello");

        repository.save(aggregate).get();

        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        ArgumentCaptor<ReplaceOptions> optsCaptor = ArgumentCaptor.forClass(ReplaceOptions.class);
        verify(collection).replaceOne(any(Bson.class), docCaptor.capture(), optsCaptor.capture());

        assertThat(docCaptor.getValue().getString("_id")).isEqualTo("id-1");
        assertThat(docCaptor.getValue().getString("name")).isEqualTo("hello");
        assertThat(optsCaptor.getValue().isUpsert()).isTrue();
    }

    @Test
    void save_whenSerializationFails_completesExceptionally() {
        TestAggregate broken = new TestAggregate("id-1", null) {
            @Override
            public String getName() {
                throw new RuntimeException("serialization boom");
            }
        };

        assertThatThrownBy(() -> repository.save(broken).get())
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("Failed to save aggregate");
    }
}
