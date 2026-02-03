package me.ronygomes.ecommerce.cart.process.queryapi;

import me.ronygomes.ecommerce.core.infrastructure.MongoClientProvider;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import tools.jackson.databind.ObjectMapper;

import static spark.Spark.*;
import static com.mongodb.client.model.Filters.eq;

public class CartQueryApi {
    public static void main(String[] args) {
        port(8085);

        MongoClient mongoClient = new MongoClientProvider().get();
        MongoDatabase database = mongoClient.getDatabase("aihackathon");
        MongoCollection<Document> cartViewCollection = database.getCollection("cart_view");

        // GET /cart?guestToken=...
        get("/cart", (req, res) -> {
            String guestToken = req.queryParams("guestToken");
            if (guestToken == null || guestToken.isBlank()) {
                res.status(400);
                return "{\"error\": \"guestToken query parameter missing\"}";
            }

            Document cart = cartViewCollection.find(eq("guestToken", guestToken)).first();

            if (cart == null) {
                res.status(404);
                return "{\"error\": \"Cart not found\"}";
            }

            cart.remove("_id");
            res.type("application/json");
            return new ObjectMapper().writeValueAsString(cart);
        });

        exception(Exception.class, (e, req, res) -> {
            res.status(500);
            res.body("{\"error\": \"" + e.getMessage() + "\"}");
        });
    }
}
