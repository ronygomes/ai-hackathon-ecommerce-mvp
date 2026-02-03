package me.ronygomes.ecommerce.core.infrastructure;

import me.ronygomes.ecommerce.core.domain.AggregateRoot;
import tools.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
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
        this.objectMapper = new ObjectMapper();
        this.aggregateClass = aggregateClass;
    }

    @Override
    public CompletableFuture<Optional<TAggregate>> getById(TId id) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = collection.find(Filters.eq("_id", id.toString())).first();
            if (doc == null)
                return Optional.empty();
            try {
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
