package me.ronygomes.ecommerce.productcatalog.presentation.queryapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.testtools.JavalinTest;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QueryApiTest {

    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> listView = mock(MongoCollection.class);
    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> detailView = mock(MongoCollection.class);
    @SuppressWarnings("unchecked")
    private final FindIterable<Document> listIterable = mock(FindIterable.class);
    @SuppressWarnings("unchecked")
    private final FindIterable<Document> detailIterable = mock(FindIterable.class);

    @BeforeEach
    void setUp() {
        when(listView.find()).thenReturn(listIterable);
        when(detailView.find(any(Bson.class))).thenReturn(detailIterable);
    }

    private Javalin setupApp() {
        return Javalin.create(config -> QueryApi.register(config, listView, detailView, new ObjectMapper()));
    }

    @Test
    void getProducts_returnsListWithoutMongoId() {
        Document p = new Document("_id", "id-1").append("name", "Widget").append("price", 9.99).append("isActive", true);
        doAnswer(invocation -> {
            ArrayList<Document> target = invocation.getArgument(0);
            target.add(p);
            return target;
        }).when(listIterable).into(any(ArrayList.class));

        JavalinTest.test(setupApp(), (server, client) -> {
            var response = client.get("/products");
            assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());
            String body = response.body().string();
            assertThat(body).contains("\"name\":\"Widget\"").doesNotContain("_id");
        });
    }

    @Test
    void getProductById_whenFound_returnsDetailWithoutMongoId() {
        Document p = new Document("_id", "id-1").append("sku", "SKU-1").append("name", "Widget");
        when(detailIterable.first()).thenReturn(p);

        JavalinTest.test(setupApp(), (server, client) -> {
            var response = client.get("/products/id-1");
            assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());
            assertThat(response.body().string()).contains("\"sku\":\"SKU-1\"").doesNotContain("_id");
        });
    }

    @Test
    void getProductById_whenNotFound_returns404() {
        when(detailIterable.first()).thenReturn(null);

        JavalinTest.test(setupApp(), (server, client) -> {
            var response = client.get("/products/missing");
            assertThat(response.code()).isEqualTo(HttpStatus.NOT_FOUND.getCode());
            assertThat(response.body().string()).contains("Product not found");
        });
    }

    @Test
    void getProducts_whenCollectionThrows_returns500ViaExceptionHandler() {
        when(listView.find()).thenThrow(new RuntimeException("mongo down"));

        JavalinTest.test(setupApp(), (server, client) -> {
            var response = client.get("/products");
            assertThat(response.code()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
            assertThat(response.body().string()).contains("mongo down");
        });
    }
}
