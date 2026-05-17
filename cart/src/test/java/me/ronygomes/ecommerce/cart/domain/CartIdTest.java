package me.ronygomes.ecommerce.cart.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartIdTest {

    @Test
    void wrapsGivenUuid() {
        UUID uuid = UUID.randomUUID();
        CartId id = new CartId(uuid);

        assertThat(id.value()).isEqualTo(uuid);
        assertThat(id.toString()).isEqualTo(uuid.toString());
    }

    @Test
    void nullValue_throws() {
        assertThatThrownBy(() -> new CartId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void generate_producesUniqueIds() {
        assertThat(CartId.generate()).isNotEqualTo(CartId.generate());
    }
}
