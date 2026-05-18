package me.ronygomes.ecommerce.checkout.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MongoSagaStateStore implements SagaStateStore {
    private static final String DB_NAME = "aihackathon";
    private static final String COLLECTION_NAME = "saga_state";

    private final MongoCollection<Document> collection;
    private final ObjectMapper objectMapper;

    public MongoSagaStateStore(MongoClient mongoClient) {
        this(mongoClient.getDatabase(DB_NAME).getCollection(COLLECTION_NAME), new ObjectMapper());
    }

    MongoSagaStateStore(MongoCollection<Document> collection, ObjectMapper objectMapper) {
        this.collection = collection;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<SagaState> findByOrderId(UUID orderId) {
        Document doc = collection.find(Filters.eq("_id", orderId.toString())).first();
        return Optional.ofNullable(doc).map(this::deserialize);
    }

    @Override
    public Optional<SagaState> findByCorrelationId(UUID correlationId) {
        Document doc = collection.find(Filters.eq("correlationId", correlationId.toString())).first();
        return Optional.ofNullable(doc).map(this::deserialize);
    }

    @Override
    public Collection<SagaState> findAll() {
        List<SagaState> states = new ArrayList<>();
        for (Document doc : collection.find()) {
            states.add(deserialize(doc));
        }
        return states;
    }

    @Override
    public void save(SagaState state) {
        try {
            String json = objectMapper.writeValueAsString(state);
            Document doc = Document.parse(json);
            doc.put("_id", state.orderId.toString());
            collection.replaceOne(Filters.eq("_id", state.orderId.toString()), doc,
                    new ReplaceOptions().upsert(true));
        } catch (Exception e) {
            throw new RuntimeException("Failed to save saga state", e);
        }
    }

    @Override
    public void remove(UUID orderId) {
        collection.deleteOne(Filters.eq("_id", orderId.toString()));
    }

    private SagaState deserialize(Document doc) {
        try {
            doc.remove("_id");
            return objectMapper.readValue(doc.toJson(), SagaState.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize saga state", e);
        }
    }
}
