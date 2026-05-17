package me.ronygomes.ecommerce.checkout.saga;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemorySagaStateStoreTest {

    private InMemorySagaStateStore store;

    @BeforeEach
    void setUp() {
        store = new InMemorySagaStateStore();
    }

    @Test
    void findByOrderId_returnsEmptyWhenNoSavedState() {
        assertThat(store.findByOrderId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void save_thenFindByOrderId_roundTrips() {
        UUID orderId = UUID.randomUUID();
        SagaState state = new SagaState(orderId, "g1", "k1");

        store.save(state);

        assertThat(store.findByOrderId(orderId)).contains(state);
    }

    @Test
    void save_overwritesExistingState() {
        UUID orderId = UUID.randomUUID();
        SagaState first = new SagaState(orderId, "g1", "k1");
        SagaState replacement = new SagaState(orderId, "g2", "k2");
        store.save(first);

        store.save(replacement);

        assertThat(store.findByOrderId(orderId)).contains(replacement);
    }

    @Test
    void findAll_returnsEverySavedState() {
        SagaState a = new SagaState(UUID.randomUUID(), "ga", "ka");
        SagaState b = new SagaState(UUID.randomUUID(), "gb", "kb");
        store.save(a);
        store.save(b);

        assertThat(store.findAll()).containsExactlyInAnyOrder(a, b);
    }

    @Test
    void remove_dropsTheMatchingState() {
        UUID orderId = UUID.randomUUID();
        store.save(new SagaState(orderId, "g1", "k1"));

        store.remove(orderId);

        assertThat(store.findByOrderId(orderId)).isEmpty();
        assertThat(store.findAll()).isEmpty();
    }

    @Test
    void remove_unknownId_isNoOp() {
        store.save(new SagaState(UUID.randomUUID(), "g1", "k1"));

        store.remove(UUID.randomUUID());

        assertThat(store.findAll()).hasSize(1);
    }
}
