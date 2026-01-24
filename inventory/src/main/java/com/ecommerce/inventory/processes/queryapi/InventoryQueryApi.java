package com.ecommerce.inventory.processes.queryapi;

import com.ecommerce.core.infrastructure.MongoClientProvider;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import tools.jackson.databind.ObjectMapper;

import static spark.Spark.*;
import static com.mongodb.client.model.Filters.eq;

public class InventoryQueryApi {
    public static void main(String[] args) {
        port(8083);

        MongoClient mongoClient = new MongoClientProvider().get();
        MongoDatabase database = mongoClient.getDatabase("aihackathon");
        MongoCollection<Document> stockAvailabilityCollection = database.getCollection("stock_availability_view");

        // GET /inventory/stock/:productId
        get("/inventory/stock/:id", (req, res) -> {
            String id = req.params(":id");
            Document stock = stockAvailabilityCollection.find(eq("_id", id)).first();

            if (stock == null) {
                res.status(404);
                return "{\"error\": \"Stock information not found\"}";
            }

            stock.remove("_id");
            res.type("application/json");
            return new ObjectMapper().writeValueAsString(stock);
        });

        exception(Exception.class, (e, req, res) -> {
            res.status(500);
            res.body("{\"error\": \"" + e.getMessage() + "\"}");
        });
    }
}
