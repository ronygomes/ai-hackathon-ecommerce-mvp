package me.ronygomes.ecommerce.inventory.presentation.queryapi;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InventoryQueryApiTest {

    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> collection = mock(MongoCollection.class);
    @SuppressWarnings("unchecked")
    private final FindIterable<Document> iterable = mock(FindIterable.class);

    @BeforeEach
    void setUp() {
        when(collection.find(any(Bson.class))).thenReturn(iterable);
    }

    private Javalin setupApp() {
        return Javalin.create(config -> InventoryQueryApi.register(config, collection, new ObjectMapper()));
    }

    @Test
    void getStockById_whenFound_returnsDocumentWithoutMongoId() {
        Document stock = new Document("_id", "id-1").append("availableQty", 12).append("inStockFlag", true);
        when(iterable.first()).thenReturn(stock);

        JavalinTest.test(setupApp(), (server, client) -> {
            var response = client.get("/inventory/stock/id-1");
            assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());
            String body = response.body().string();
            assertThat(body).contains("\"availableQty\":12").doesNotContain("_id");
        });
    }

    @Test
    void getStockById_whenNotFound_returns404() {
        when(iterable.first()).thenReturn(null);

        JavalinTest.test(setupApp(), (server, client) -> {
            var response = client.get("/inventory/stock/missing");
            assertThat(response.code()).isEqualTo(HttpStatus.NOT_FOUND.getCode());
            assertThat(response.body().string()).contains("Stock information not found");
        });
    }

    @Test
    void getStockById_whenCollectionThrows_returns500ViaExceptionHandler() {
        when(collection.find(any(Bson.class))).thenThrow(new RuntimeException("mongo down"));

        JavalinTest.test(setupApp(), (server, client) -> {
            var response = client.get("/inventory/stock/anything");
            assertThat(response.code()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
            assertThat(response.body().string()).contains("mongo down");
        });
    }
}
