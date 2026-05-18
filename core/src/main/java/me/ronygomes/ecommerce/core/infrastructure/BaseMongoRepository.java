package me.ronygomes.ecommerce.core.infrastructure;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import me.ronygomes.ecommerce.core.domain.AggregateRoot;
import org.bson.Document;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public abstract class BaseMongoRepository<TAggregate extends AggregateRoot<TId>, TId>
        implements Repository<TAggregate, TId> {
    protected final MongoCollection<Document> collection;
    protected final ObjectMapper objectMapper;
    protected final Class<TAggregate> aggregateClass;

    protected BaseMongoRepository(MongoClient mongoClient, String dbName, String collectionName,
            Class<TAggregate> aggregateClass) {
        MongoDatabase database = mongoClient.getDatabase(dbName);
        this.collection = database.getCollection(collectionName);
        this.objectMapper = aggregateMapper();
        this.aggregateClass = aggregateClass;
    }

    /**
     * ObjectMapper tuned for aggregate persistence: drives serialization off the
     * private fields (so private no-arg ctors + immutable accessors round-trip
     * cleanly) and tolerates unknown properties so old documents survive schema
     * evolution.
     */
    private static ObjectMapper aggregateMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        mapper.setVisibility(PropertyAccessor.GETTER, Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.IS_GETTER, Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.SETTER, Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.CREATOR, Visibility.ANY);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    @Override
    public CompletableFuture<Optional<TAggregate>> getById(TId id) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = collection.find(Filters.eq("_id", id.toString())).first();
            if (doc == null)
                return Optional.empty();
            try {
                doc.remove("_id");
                return Optional.of(objectMapper.readValue(doc.toJson(), aggregateClass));
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize aggregate", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> save(TAggregate aggregate) {
        return CompletableFuture.runAsync(() -> {
            try {
                String json = objectMapper.writeValueAsString(aggregate);
                Document doc = Document.parse(json);
                doc.put("_id", aggregate.getId().toString()); // Ensure ID is used as MongoDB _id
                collection.replaceOne(Filters.eq("_id", aggregate.getId().toString()), doc,
                        new ReplaceOptions().upsert(true));
            } catch (Exception e) {
                throw new RuntimeException("Failed to save aggregate", e);
            }
        });
    }
}
