package com.ecommerce.core.infrastructure;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class MongoClientProvider implements Provider<MongoClient> {
    private final MongoClient mongoClient;

    public MongoClientProvider() {
        this.mongoClient = MongoClients.create("mongodb://admin:admin@localhost:27017");
    }

    @Override
    public MongoClient get() {
        return mongoClient;
    }
}
