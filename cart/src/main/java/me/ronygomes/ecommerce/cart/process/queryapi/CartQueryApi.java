package me.ronygomes.ecommerce.cart.process.queryapi;

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

public class CartQueryApi {
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
        }).start(8085);

        MongoClient mongoClient = new MongoClientProvider().get();
        MongoDatabase database = mongoClient.getDatabase("aihackathon");
        MongoCollection<Document> cartViewCollection = database.getCollection("cart_view");

        // GET /cart?guestToken=...
        app.get("/cart", ctx -> {
            String guestToken = ctx.queryParam("guestToken");
            if (guestToken == null || guestToken.isBlank()) {
                ctx.status(400);
                ctx.result("{\"error\": \"guestToken query parameter missing\"}");
                return;
            }

            Document cart = cartViewCollection.find(eq("guestToken", guestToken)).first();

            if (cart == null) {
                ctx.status(404);
                ctx.result("{\"error\": \"Cart not found\"}");
                return;
            }

            cart.remove("_id");
            ctx.contentType("application/json");
            ctx.result(new ObjectMapper().writeValueAsString(cart));
        });

        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(500);
            ctx.result("{\"error\": \"" + e.getMessage() + "\"}");
        });
    }
}
