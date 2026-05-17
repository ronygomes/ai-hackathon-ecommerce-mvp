package me.ronygomes.ecommerce.ordering.presentation.queryapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import me.ronygomes.ecommerce.core.infrastructure.MongoClientProvider;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class OrderingQueryApi {

    public static void register(JavalinConfig config,
                                MongoCollection<Document> orderProjections,
                                ObjectMapper objectMapper) {

        config.routes.get("/orders/{guestToken}", ctx -> {
            String guestToken = ctx.pathParam("guestToken");
            List<Document> orders = orderProjections.find(Filters.eq("guestToken", guestToken)).into(new ArrayList<>());
            ctx.contentType("application/json");
            ctx.result(objectMapper.writeValueAsString(orders));
        });

        config.routes.get("/orders/id/{orderId}", ctx -> {
            String orderId = ctx.pathParam("orderId");
            Document order = orderProjections.find(Filters.eq("_id", orderId)).first();
            ctx.contentType("application/json");
            ctx.result(order != null ? objectMapper.writeValueAsString(order) : "{}");
        });
    }

    static void main() {
        MongoClient mongoClient = new MongoClientProvider().get();
        MongoCollection<Document> orderProjections = mongoClient.getDatabase("aihackathon")
                .getCollection("order_projections");
        ObjectMapper objectMapper = new ObjectMapper();

        Javalin.create(config -> register(config, orderProjections, objectMapper)).start(8087);
    }
}
