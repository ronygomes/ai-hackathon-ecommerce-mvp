package me.rongyomes.ecommerce.checkout.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.rongyomes.ecommerce.checkout.saga.message.event.CartSnapshotProvided;
import me.rongyomes.ecommerce.checkout.saga.message.event.ProductSnapshotsProvided;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SagaStateTest {

    @Test
    void construct_initialisesCorrelationFieldsAndDefaults() {
        UUID orderId = UUID.randomUUID();

        SagaState state = new SagaState(orderId, "guest-1", "idem-1");

        assertThat(state.orderId).isEqualTo(orderId);
        assertThat(state.guestToken).isEqualTo("guest-1");
        assertThat(state.idempotencyKey).isEqualTo("idem-1");
        assertThat(state.stockValidated).isFalse();
        assertThat(state.totalItemsToDeduct).isZero();
        assertThat(state.deductedItemsCount).isZero();
        assertThat(state.cartItems).isNull();
        assertThat(state.productSnapshots).isNull();
    }

    @Test
    void jacksonRoundTrip_preservesAllFields() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        SagaState state = new SagaState(orderId, "g1", "k1");
        state.stockValidated = true;
        state.totalItemsToDeduct = 3;
        state.deductedItemsCount = 2;
        state.cartItems = List.of(new CartSnapshotProvided.CartItemSnapshot(productId, 4));
        state.productSnapshots = List.of(new ProductSnapshotsProvided.ProductSnapshot(productId, "SKU", "Widget", 9.99, true));

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(state);
        SagaState roundTripped = mapper.readValue(json, SagaState.class);

        assertThat(roundTripped.orderId).isEqualTo(orderId);
        assertThat(roundTripped.guestToken).isEqualTo("g1");
        assertThat(roundTripped.idempotencyKey).isEqualTo("k1");
        assertThat(roundTripped.stockValidated).isTrue();
        assertThat(roundTripped.totalItemsToDeduct).isEqualTo(3);
        assertThat(roundTripped.deductedItemsCount).isEqualTo(2);
        assertThat(roundTripped.cartItems).singleElement().satisfies(item -> {
            assertThat(item.productId()).isEqualTo(productId);
            assertThat(item.qty()).isEqualTo(4);
        });
        assertThat(roundTripped.productSnapshots).singleElement().satisfies(snap -> {
            assertThat(snap.sku()).isEqualTo("SKU");
            assertThat(snap.price()).isEqualTo(9.99);
        });
    }
}
