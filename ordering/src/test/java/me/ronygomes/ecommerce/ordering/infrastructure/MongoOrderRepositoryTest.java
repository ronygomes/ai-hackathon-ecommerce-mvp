package me.ronygomes.ecommerce.ordering.infrastructure;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import me.ronygomes.ecommerce.ordering.domain.CustomerInfo;
import me.ronygomes.ecommerce.ordering.domain.GuestToken;
import me.ronygomes.ecommerce.ordering.domain.IdempotencyKey;
import me.ronygomes.ecommerce.ordering.domain.Order;
import me.ronygomes.ecommerce.ordering.domain.OrderId;
import me.ronygomes.ecommerce.ordering.domain.OrderLineItem;
import me.ronygomes.ecommerce.ordering.domain.OrderStatus;
import me.ronygomes.ecommerce.ordering.domain.ShippingAddress;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MongoOrderRepositoryTest {

    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> collection = mock(MongoCollection.class);
    @SuppressWarnings("unchecked")
    private final FindIterable<Document> iterable = mock(FindIterable.class);
    private MongoOrderRepository repository;

    @BeforeEach
    void setUp() {
        MongoClient client = mock(MongoClient.class);
        MongoDatabase database = mock(MongoDatabase.class);
        when(client.getDatabase("aihackathon")).thenReturn(database);
        when(database.getCollection("orders")).thenReturn(collection);
        when(collection.find(any(Bson.class))).thenReturn(iterable);

        repository = new MongoOrderRepository(client);
    }

    @Test
    void getByIdempotencyKey_whenNotFound_returnsEmpty() throws Exception {
        when(iterable.first()).thenReturn(null);

        Optional<Order> result = repository.getByIdempotencyKey(new IdempotencyKey(UUID.randomUUID())).get();

        assertThat(result).isEmpty();
    }

    @Test
    void saveThenLoad_roundTripsAggregateState() throws Exception {
        OrderId orderId = OrderId.generate();
        IdempotencyKey idempotencyKey = new IdempotencyKey(UUID.randomUUID());
        UUID productId = UUID.randomUUID();
        Order original = Order.place(
                orderId,
                new GuestToken("g1"),
                new CustomerInfo("Alice", "+1-555", "alice@example.com"),
                new ShippingAddress("1 Main", "Anytown", "12345", "USA"),
                List.of(new OrderLineItem(productId, "SKU-1", "Widget", 9.99, 2)),
                idempotencyKey);

        repository.save(original).get();

        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(collection).replaceOne(any(Bson.class), docCaptor.capture(), any(ReplaceOptions.class));
        when(iterable.first()).thenReturn(docCaptor.getValue());

        Optional<Order> reloaded = repository.getByIdempotencyKey(idempotencyKey).get();

        assertThat(reloaded).hasValueSatisfying(order -> {
            assertThat(order.getId()).isEqualTo(orderId);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
            assertThat(order.getOrderNumber().value()).isEqualTo(original.getOrderNumber().value());
        });
    }
}
