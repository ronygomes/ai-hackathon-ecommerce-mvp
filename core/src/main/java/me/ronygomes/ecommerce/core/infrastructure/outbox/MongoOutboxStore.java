package me.ronygomes.ecommerce.core.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import me.ronygomes.ecommerce.core.domain.DomainEvent;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class MongoOutboxStore implements OutboxStore {

    private final MongoCollection<Document> collection;
    private final ObjectMapper objectMapper;

    public MongoOutboxStore(MongoClient client, String dbName, String collectionName) {
        this(client.getDatabase(dbName).getCollection(collectionName), new ObjectMapper());
    }

    MongoOutboxStore(MongoCollection<Document> collection, ObjectMapper objectMapper) {
        this.collection = collection;
        this.objectMapper = objectMapper;
    }

    @Override
    public void append(String aggregateId, List<DomainEvent> events) {
        if (events.isEmpty()) return;
        List<Document> docs = new ArrayList<>(events.size());
        for (DomainEvent event : events) {
            docs.add(toDoc(aggregateId, event));
        }
        collection.insertMany(docs);
    }

    @Override
    public List<OutboxEntry> findPending(int limit) {
        List<OutboxEntry> entries = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("publishedAt", null))
                .sort(Sorts.ascending("createdAt"))
                .limit(limit)) {
            entries.add(fromDoc(doc));
        }
        return entries;
    }

    @Override
    public void markPublished(List<String> ids) {
        if (ids.isEmpty()) return;
        collection.updateMany(Filters.in("_id", ids),
                Updates.set("publishedAt", System.currentTimeMillis()));
    }

    private Document toDoc(String aggregateId, DomainEvent event) {
        try {
            OutboxEntry entry = OutboxEntry.pending(
                    aggregateId,
                    event.getClass().getSimpleName(),
                    objectMapper.writeValueAsString(event));
            return new Document()
                    .append("_id", entry.id())
                    .append("aggregateId", entry.aggregateId())
                    .append("eventType", entry.eventType())
                    .append("payload", entry.payload())
                    .append("createdAt", entry.createdAt())
                    .append("publishedAt", entry.publishedAt());
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize outbox event", e);
        }
    }

    private OutboxEntry fromDoc(Document doc) {
        return new OutboxEntry(
                doc.getString("_id"),
                doc.getString("aggregateId"),
                doc.getString("eventType"),
                doc.getString("payload"),
                doc.getLong("createdAt"),
                doc.get("publishedAt") == null ? null : doc.getLong("publishedAt"));
    }
}
