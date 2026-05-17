package me.ronygomes.ecommerce.cart.presentation.queryapi;

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

class CartQueryApiTest {

    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> collection = mock(MongoCollection.class);
    @SuppressWarnings("unchecked")
    private final FindIterable<Document> iterable = mock(FindIterable.class);

    @BeforeEach
    void setUp() {
        when(collection.find(any(Bson.class))).thenReturn(iterable);
    }

    private Javalin setupApp() {
        return Javalin.create(config -> CartQueryApi.register(config, collection, new ObjectMapper()));
    }

    @Test
    void getCart_missingGuestTokenQueryParam_returns400() {
        JavalinTest.test(setupApp(), (server, client) -> {
            var response = client.get("/cart");
            assertThat(response.code()).isEqualTo(HttpStatus.BAD_REQUEST.getCode());
            assertThat(response.body().string()).contains("guestToken query parameter missing");
        });
    }

    @Test
    void getCart_whenFound_returnsDocumentWithoutMongoId() {
        Document cart = new Document("_id", "cart-1").append("guestToken", "g1").append("items", java.util.List.of());
        when(iterable.first()).thenReturn(cart);

        JavalinTest.test(setupApp(), (server, client) -> {
            var response = client.get("/cart?guestToken=g1");
            assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());
            String body = response.body().string();
            assertThat(body).contains("\"guestToken\":\"g1\"").doesNotContain("_id");
        });
    }

    @Test
    void getCart_whenNotFound_returns404() {
        when(iterable.first()).thenReturn(null);

        JavalinTest.test(setupApp(), (server, client) -> {
            var response = client.get("/cart?guestToken=missing");
            assertThat(response.code()).isEqualTo(HttpStatus.NOT_FOUND.getCode());
            assertThat(response.body().string()).contains("Cart not found");
        });
    }

    @Test
    void getCart_whenCollectionThrows_returns500ViaExceptionHandler() {
        when(collection.find(any(Bson.class))).thenThrow(new RuntimeException("mongo down"));

        JavalinTest.test(setupApp(), (server, client) -> {
            var response = client.get("/cart?guestToken=g1");
            assertThat(response.code()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
            assertThat(response.body().string()).contains("mongo down");
        });
    }
}
