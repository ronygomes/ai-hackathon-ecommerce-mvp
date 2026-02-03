package me.ronygomes.ecommerce.ordering.process.queryapi;

import io.javalin.Javalin;
import tools.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import me.ronygomes.ecommerce.core.infrastructure.MongoClientProvider;
import org.bson.Document;
import java.util.ArrayList;
import java.util.List;

public class OrderingQueryApi {
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
        }).start(8087);
        MongoClient mongoClient = new MongoClientProvider().get();
        MongoCollection<Document> collection = mongoClient.getDatabase("aihackathon")
                .getCollection("order_projections");
        ObjectMapper objectMapper = new ObjectMapper();

        app.get("/orders/{guestToken}", ctx -> {
            String guestToken = ctx.pathParam("guestToken");
            List<Document> orders = collection.find(Filters.eq("guestToken", guestToken)).into(new ArrayList<>());
            ctx.contentType("application/json");
            ctx.result(objectMapper.writeValueAsString(orders));
        });

        app.get("/orders/id/{orderId}", ctx -> {
            String orderId = ctx.pathParam("orderId");
            Document order = collection.find(Filters.eq("_id", orderId)).first();
            ctx.contentType("application/json");
            ctx.result(order != null ? objectMapper.writeValueAsString(order) : "{}");
        });
    }
}
