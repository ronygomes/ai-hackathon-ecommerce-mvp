package com.ecommerce.ordering.processes.queryapi;

import spark.Spark;
import tools.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.ecommerce.core.infrastructure.MongoClientProvider;
import org.bson.Document;
import java.util.ArrayList;
import java.util.List;

public class OrderingQueryApi {
    public static void main(String[] args) {
        Spark.port(8087);
        MongoClient mongoClient = new MongoClientProvider().get();
        MongoCollection<Document> collection = mongoClient.getDatabase("aihackathon")
                .getCollection("order_projections");
        ObjectMapper objectMapper = new ObjectMapper();

        Spark.get("/orders/:guestToken", (req, res) -> {
            String guestToken = req.params(":guestToken");
            List<Document> orders = collection.find(Filters.eq("guestToken", guestToken)).into(new ArrayList<>());
            res.type("application/json");
            return objectMapper.writeValueAsString(orders);
        });

        Spark.get("/orders/id/:orderId", (req, res) -> {
            String orderId = req.params(":orderId");
            Document order = collection.find(Filters.eq("_id", orderId)).first();
            res.type("application/json");
            return order != null ? objectMapper.writeValueAsString(order) : "{}";
        });
    }
}
