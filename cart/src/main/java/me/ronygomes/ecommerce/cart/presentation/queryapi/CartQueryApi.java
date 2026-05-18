package me.ronygomes.ecommerce.cart.presentation.queryapi;

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

public class CartQueryApi {

    public static void register(JavalinConfig config,
                                MongoCollection<Document> cartView,
                                ObjectMapper objectMapper) {

        config.routes.get("/cart", ctx -> {
            String guestToken = ctx.queryParam("guestToken");
            if (guestToken == null || guestToken.isBlank()) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.result("{\"error\": \"guestToken query parameter missing\"}");
                return;
            }

            Document cart = cartView.find(eq("guestToken", guestToken)).first();

            if (cart == null) {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.result("{\"error\": \"Cart not found\"}");
                return;
            }

            cart.remove("_id");
            ctx.contentType("application/json");
            ctx.result(objectMapper.writeValueAsString(cart));
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
        MongoCollection<Document> cartView = database.getCollection("cart_view");
        ObjectMapper objectMapper = new ObjectMapper();

        Javalin.create(cfg -> register(cfg, cartView, objectMapper)).start(8085);
    }
}
