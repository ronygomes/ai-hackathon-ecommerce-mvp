package me.rongyomes.ecommerce.checkout.saga;

import org.junit.jupiter.api.Test;

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
}
