package me.ronygomes.ecommerce.productcatalog.process.queryapi;

import me.ronygomes.ecommerce.core.infrastructure.MongoClientProvider;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import io.javalin.Javalin;
import org.bson.Document;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

public class QueryApi {
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
        }).start(8081);

        MongoClient mongoClient = new MongoClientProvider().get();
        MongoDatabase database = mongoClient.getDatabase("aihackathon");

        // Two separate collections for projections
        MongoCollection<Document> listViewCollection = database.getCollection("product_list_view");
        MongoCollection<Document> detailViewCollection = database.getCollection("product_detail_view");

        // GET /products uses ProductListView
        app.get("/products", ctx -> {
            List<Document> products = new ArrayList<>();
            listViewCollection.find().into(products);

            // Redact _id for the response, using the productId field
            List<Document> response = products.stream().map(p -> {
                p.remove("_id");
                return p;
            }).toList();

            ctx.contentType("application/json");
            ctx.result(new ObjectMapper().writeValueAsString(response));
        });

        // GET /products/:id uses ProductDetailView
        app.get("/products/{id}", ctx -> {
            String id = ctx.pathParam("id");
            Document product = detailViewCollection.find(eq("_id", id)).first();

            if (product == null) {
                ctx.status(404);
                ctx.result("{\"error\": \"Product not found\"}");
                return;
            }

            // Redact _id for the response
            product.remove("_id");

            ctx.contentType("application/json");
            ctx.result(new ObjectMapper().writeValueAsString(product));
        });

        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(500);
            ctx.result("{\"error\": \"" + e.getMessage() + "\"}");
        });
    }
}
