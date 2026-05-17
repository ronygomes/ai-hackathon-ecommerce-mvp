package me.ronygomes.ecommerce.ordering.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderIdTest {

    @Test
    void wrapsGivenUuid() {
        UUID uuid = UUID.randomUUID();
        OrderId id = new OrderId(uuid);

        assertThat(id.value()).isEqualTo(uuid);
        assertThat(id.toString()).isEqualTo(uuid.toString());
    }

    @Test
    void nullValue_throws() {
        assertThatThrownBy(() -> new OrderId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void generate_producesUniqueIds() {
        assertThat(OrderId.generate()).isNotEqualTo(OrderId.generate());
    }

    @Test
    void fromString_roundTrips() {
        UUID uuid = UUID.randomUUID();

        assertThat(OrderId.fromString(uuid.toString()).value()).isEqualTo(uuid);
    }

    @Test
    void fromString_invalidUuid_throws() {
        assertThatThrownBy(() -> OrderId.fromString("nope"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
