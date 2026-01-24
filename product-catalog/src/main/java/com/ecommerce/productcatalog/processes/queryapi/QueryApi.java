package com.ecommerce.productcatalog.processes.queryapi;

import com.ecommerce.core.infrastructure.MongoClientProvider;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import static spark.Spark.*;
import static com.mongodb.client.model.Filters.eq;

public class QueryApi {
    public static void main(String[] args) {
        port(8081);

        MongoClient mongoClient = new MongoClientProvider().get();
        MongoDatabase database = mongoClient.getDatabase("aihackathon");

        // Two separate collections for projections
        MongoCollection<Document> listViewCollection = database.getCollection("product_list_view");
        MongoCollection<Document> detailViewCollection = database.getCollection("product_detail_view");

        // GET /products uses ProductListView
        get("/products", (req, res) -> {
            List<Document> products = new ArrayList<>();
            listViewCollection.find().into(products);

            // Redact _id for the response, using the productId field
            List<Document> response = products.stream().map(p -> {
                p.remove("_id");
                return p;
            }).toList();

            res.type("application/json");
            return new ObjectMapper().writeValueAsString(response);
        });

        // GET /products/:id uses ProductDetailView
        get("/products/:id", (req, res) -> {
            String id = req.params(":id");
            Document product = detailViewCollection.find(eq("_id", id)).first();

            if (product == null) {
                res.status(404);
                return "{\"error\": \"Product not found\"}";
            }

            // Redact _id for the response
            product.remove("_id");

            res.type("application/json");
            return new ObjectMapper().writeValueAsString(product);
        });

        exception(Exception.class, (e, req, res) -> {
            res.status(500);
            res.body("{\"error\": \"" + e.getMessage() + "\"}");
        });
    }
}
