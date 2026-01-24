package com.ecommerce.ordering.processes.queryapi;

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

public class OrderingQueryApi {
    public static void main(String[] args) {
        port(8087);

        MongoClient mongoClient = new MongoClientProvider().get();
        MongoDatabase database = mongoClient.getDatabase("aihackathon");

        MongoCollection<Document> detailViewCollection = database.getCollection("order_detail_view");
        MongoCollection<Document> adminListViewCollection = database.getCollection("admin_order_list_view");

        // GET /orders/:orderIdOrNumber (Customer)
        get("/orders/:id", (req, res) -> {
            String id = req.params(":id");
            Document order = detailViewCollection.find(
                    com.mongodb.client.model.Filters.or(eq("_id", id), eq("orderNumber", id))).first();

            if (order == null) {
                res.status(404);
                return "{\"error\": \"Order not found\"}";
            }

            order.remove("_id");
            res.type("application/json");
            return new ObjectMapper().writeValueAsString(order);
        });

        // GET /admin/orders (Admin)
        get("/admin/orders", (req, res) -> {
            List<Document> orders = adminListViewCollection.find().into(new ArrayList<>());
            for (Document d : orders)
                d.remove("_id");
            res.type("application/json");
            return new ObjectMapper().writeValueAsString(orders);
        });

        exception(Exception.class, (e, req, res) -> {
            res.status(500);
            res.body("{\"error\": \"" + e.getMessage() + "\"}");
        });
    }
}
