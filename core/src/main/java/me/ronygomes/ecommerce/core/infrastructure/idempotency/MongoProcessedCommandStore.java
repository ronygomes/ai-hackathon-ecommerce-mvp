package me.ronygomes.ecommerce.core.infrastructure.idempotency;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;

public class MongoProcessedCommandStore implements ProcessedCommandStore {

    private final MongoCollection<Document> collection;

    public MongoProcessedCommandStore(MongoClient client, String dbName, String collectionName) {
        this(client.getDatabase(dbName).getCollection(collectionName));
    }

    public MongoProcessedCommandStore(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    @Override
    public boolean wasProcessed(String commandId) {
        return collection.find(Filters.eq("_id", commandId)).first() != null;
    }

    @Override
    public void markProcessed(String commandId, String metadata) {
        Document doc = new Document("_id", commandId)
                .append("metadata", metadata)
                .append("processedAt", System.currentTimeMillis());
        try {
            collection.insertOne(doc);
        } catch (MongoWriteException e) {
            // Duplicate key on _id means the command was already marked (i.e. idempotent no-op)
            if (e.getError().getCategory() != ErrorCategory.DUPLICATE_KEY) {
                throw e;
            }
        }
    }
}
