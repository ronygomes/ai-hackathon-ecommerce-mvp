package me.ronygomes.ecommerce.inventory.presentation.queryapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import me.ronygomes.ecommerce.core.infrastructure.MongoClientProvider;
import org.bson.Document;

import static com.mongodb.client.model.Filters.eq;

public class InventoryQueryApi {
    static void main() {
        MongoClient mongoClient = new MongoClientProvider().get();
        MongoDatabase database = mongoClient.getDatabase("aihackathon");
        MongoCollection<Document> stockAvailabilityCollection = database.getCollection("stock_availability_view");

        ObjectMapper objectMapper = new ObjectMapper();

        Javalin.create(config -> {
            // GET /inventory/stock/{id}
            config.routes.get("/inventory/stock/{id}", ctx -> {
                String id = ctx.pathParam("id");
                Document stock = stockAvailabilityCollection.find(eq("_id", id)).first();

                if (stock == null) {
                    ctx.status(HttpStatus.NOT_FOUND);
                    ctx.result("{\"error\": \"Stock information not found\"}");
                    return;
                }

                stock.remove("_id");
                ctx.contentType("application/json");
                ctx.result(objectMapper.writeValueAsString(stock));
            });

            config.routes.exception(Exception.class, (e, ctx) -> {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                ctx.result("{\"error\": \"" + e.getMessage() + "\"}");
            });
        }).start(8083);
    }
}
