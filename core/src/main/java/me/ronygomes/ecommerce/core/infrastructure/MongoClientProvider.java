package me.ronygomes.ecommerce.core.infrastructure;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

@Singleton
public class MongoClientProvider implements Provider<MongoClient> {
    private final MongoClient mongoClient;

    @Inject
    public MongoClientProvider(AppConfig config) {
        this.mongoClient = MongoClients.create(config.mongoUri());
    }

    @Override
    public MongoClient get() {
        return mongoClient;
    }
}
