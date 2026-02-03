package me.ronygomes.ecommerce.inventory.process.queryapi;

import me.ronygomes.ecommerce.core.infrastructure.MongoClientProvider;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import tools.jackson.databind.ObjectMapper;

import io.javalin.Javalin;
import org.bson.Document;
import tools.jackson.databind.ObjectMapper;

import static com.mongodb.client.model.Filters.eq;

public class InventoryQueryApi {
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
        }).start(8083);

        MongoClient mongoClient = new MongoClientProvider().get();
        MongoDatabase database = mongoClient.getDatabase("aihackathon");
        MongoCollection<Document> stockAvailabilityCollection = database.getCollection("stock_availability_view");

        // GET /inventory/stock/{id}
        app.get("/inventory/stock/{id}", ctx -> {
            String id = ctx.pathParam("id");
            Document stock = stockAvailabilityCollection.find(eq("_id", id)).first();

            if (stock == null) {
                ctx.status(404);
                ctx.result("{\"error\": \"Stock information not found\"}");
                return;
            }

            stock.remove("_id");
            ctx.contentType("application/json");
            ctx.result(new ObjectMapper().writeValueAsString(stock));
        });

        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(500);
            ctx.result("{\"error\": \"" + e.getMessage() + "\"}");
        });
    }
}
