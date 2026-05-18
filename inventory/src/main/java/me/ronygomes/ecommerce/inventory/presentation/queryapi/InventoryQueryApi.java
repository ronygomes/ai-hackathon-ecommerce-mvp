package me.ronygomes.ecommerce.inventory.presentation.queryapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.HttpStatus;
import me.ronygomes.ecommerce.core.infrastructure.AppConfig;
import me.ronygomes.ecommerce.core.infrastructure.MongoClientProvider;
import org.bson.Document;

import static com.mongodb.client.model.Filters.eq;

public class InventoryQueryApi {

    public static void register(JavalinConfig config,
                                MongoCollection<Document> stockAvailability,
                                ObjectMapper objectMapper) {

        config.routes.get("/inventory/stock/{id}", ctx -> {
            String id = ctx.pathParam("id");
            Document stock = stockAvailability.find(eq("_id", id)).first();

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
    }

    static void main() {
        AppConfig config = AppConfig.fromEnv();
        MongoClient mongoClient = new MongoClientProvider(config).get();
        MongoDatabase database = mongoClient.getDatabase(config.mongoDbName());
        MongoCollection<Document> stockAvailability = database.getCollection("stock_availability_view");
        ObjectMapper objectMapper = new ObjectMapper();

        Javalin.create(cfg -> register(cfg, stockAvailability, objectMapper)).start(8083);
    }
}
