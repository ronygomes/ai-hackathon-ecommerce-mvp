package me.ronygomes.ecommerce.cart.infrastructure;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import me.ronygomes.ecommerce.cart.domain.CartId;
import me.ronygomes.ecommerce.core.infrastructure.AppConfig;

import me.ronygomes.ecommerce.cart.domain.GuestToken;
import me.ronygomes.ecommerce.cart.domain.ProductId;
import me.ronygomes.ecommerce.cart.domain.Quantity;
import me.ronygomes.ecommerce.cart.domain.ShoppingCart;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MongoCartRepositoryTest {

    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> collection = mock(MongoCollection.class);
    @SuppressWarnings("unchecked")
    private final FindIterable<Document> iterable = mock(FindIterable.class);
    private MongoCartRepository repository;

    @BeforeEach
    void setUp() {
        MongoClient client = mock(MongoClient.class);
        MongoDatabase database = mock(MongoDatabase.class);
        AppConfig config = new AppConfig("mongodb://test", "test-db", "localhost");
        when(client.getDatabase("test-db")).thenReturn(database);
        when(database.getCollection("carts")).thenReturn(collection);
        when(collection.find(any(Bson.class))).thenReturn(iterable);

        repository = new MongoCartRepository(client, config);
    }

    @Test
    void getByGuestToken_whenNotFound_returnsEmpty() throws Exception {
        when(iterable.first()).thenReturn(null);

        Optional<ShoppingCart> result = repository.getByGuestToken(new GuestToken("g1")).get();

        assertThat(result).isEmpty();
    }

    @Test
    void saveThenLoad_roundTripsAggregateState() throws Exception {
        CartId cartId = CartId.generate();
        ShoppingCart original = ShoppingCart.create(cartId, new GuestToken("g1"));
        ProductId productId = new ProductId(UUID.randomUUID());
        original.addItem(productId, new Quantity(3));

        repository.save(original).get();

        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(collection).replaceOne(any(Bson.class), docCaptor.capture(), any(ReplaceOptions.class));
        when(iterable.first()).thenReturn(docCaptor.getValue());

        Optional<ShoppingCart> reloaded = repository.getByGuestToken(new GuestToken("g1")).get();

        assertThat(reloaded).hasValueSatisfying(cart -> {
            assertThat(cart.getId()).isEqualTo(cartId);
            assertThat(cart.getGuestToken().value()).isEqualTo("g1");
            assertThat(cart.getItems()).singleElement().satisfies(item -> {
                assertThat(item.getProductId()).isEqualTo(productId);
                assertThat(item.getQuantity().value()).isEqualTo(3);
            });
        });
    }
}
