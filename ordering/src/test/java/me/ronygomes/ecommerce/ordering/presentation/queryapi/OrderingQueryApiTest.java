package me.ronygomes.ecommerce.ordering.presentation.queryapi;

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

class OrderingQueryApiTest {

    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> collection = mock(MongoCollection.class);
    @SuppressWarnings("unchecked")
    private final FindIterable<Document> iterable = mock(FindIterable.class);

    @BeforeEach
    void setUp() {
        when(collection.find(any(Bson.class))).thenReturn(iterable);
    }

    private Javalin setupApp() {
        return Javalin.create(config -> OrderingQueryApi.register(config, collection, new ObjectMapper()));
    }

    @Test
    void getOrdersByGuestToken_returnsJsonArrayOfMatchingOrders() {
        Document order = new Document("_id", "id-1").append("guestToken", "g1").append("status", "CONFIRMED");
        doAnswer(inv -> {
            ArrayList<Document> target = inv.getArgument(0);
            target.add(order);
            return target;
        }).when(iterable).into(any(ArrayList.class));

        JavalinTest.test(setupApp(), (server, client) -> {
            var response = client.get("/orders/g1");
            assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());
            assertThat(response.body().string()).contains("\"guestToken\":\"g1\"");
        });
    }

    @Test
    void getOrderById_whenFound_returnsOrderJson() {
        Document order = new Document("_id", "id-1").append("guestToken", "g1");
        when(iterable.first()).thenReturn(order);

        JavalinTest.test(setupApp(), (server, client) -> {
            var response = client.get("/orders/id/id-1");
            assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());
            assertThat(response.body().string()).contains("\"_id\":\"id-1\"");
        });
    }

    @Test
    void getOrderById_whenNotFound_returns404() {
        when(iterable.first()).thenReturn(null);

        JavalinTest.test(setupApp(), (server, client) -> {
            var response = client.get("/orders/id/missing");
            assertThat(response.code()).isEqualTo(HttpStatus.NOT_FOUND.getCode());
            assertThat(response.body().string()).contains("Order not found");
        });
    }
}
