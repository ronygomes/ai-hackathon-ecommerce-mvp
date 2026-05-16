package me.ronygomes.ecommerce.productcatalog.presentation.queryapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import me.ronygomes.ecommerce.core.infrastructure.MongoClientProvider;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

public class QueryApi {
    static void main() {
        MongoClient mongoClient = new MongoClientProvider().get();
        MongoDatabase database = mongoClient.getDatabase("aihackathon");

        // Two separate collections for projections
        MongoCollection<Document> listViewCollection = database.getCollection("product_list_view");
        MongoCollection<Document> detailViewCollection = database.getCollection("product_detail_view");

        ObjectMapper objectMapper = new ObjectMapper();

        Javalin.create(config -> {
            // GET /products uses ProductListView
            config.routes.get("/products", ctx -> {
                List<Document> products = new ArrayList<>();
                listViewCollection.find().into(products);

                // Redact _id for the response, using the productId field
                List<Document> response = products.stream().map(p -> {
                    p.remove("_id");
                    return p;
                }).toList();

                ctx.contentType("application/json");
                ctx.result(objectMapper.writeValueAsString(response));
            });

            // GET /products/:id uses ProductDetailView
            config.routes.get("/products/{id}", ctx -> {
                String id = ctx.pathParam("id");
                Document product = detailViewCollection.find(eq("_id", id)).first();

                if (product == null) {
                    ctx.status(HttpStatus.NOT_FOUND);
                    ctx.result("{\"error\": \"Product not found\"}");
                    return;
                }

                // Redact _id for the response
                product.remove("_id");

                ctx.contentType("application/json");
                ctx.result(objectMapper.writeValueAsString(product));
            });

            config.routes.exception(Exception.class, (e, ctx) -> {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                ctx.result("{\"error\": \"" + e.getMessage() + "\"}");
            });
        }).start(8081);
    }
}
