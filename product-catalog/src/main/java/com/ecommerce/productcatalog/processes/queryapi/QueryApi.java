package com.ecommerce.productcatalog.processes.queryapi;

import com.ecommerce.core.infrastructure.MongoClientProvider;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import static spark.Spark.get;
import static spark.Spark.port;

public class QueryApi {
    public static void main(String[] args) {
        port(8081);

        MongoClient mongoClient = new MongoClientProvider().get();
        MongoDatabase database = mongoClient.getDatabase("aihackathon");
        MongoCollection<Document> productsCollection = database.getCollection("products_view");

        get("/products", (req, res) -> {
            List<Document> products = new ArrayList<>();
            productsCollection.find().into(products);
            res.type("application/json");
            return new ObjectMapper().writeValueAsString(products);
        });

        get("/products/:id", (req, res) -> {
            Document doc = productsCollection.find(new Document("_id", req.params(":id"))).first();
            if (doc == null) {
                res.status(404);
                return "{\"error\": \"Product not found\"}";
            }
            res.type("application/json");
            return doc.toJson();
        });
    }
}
