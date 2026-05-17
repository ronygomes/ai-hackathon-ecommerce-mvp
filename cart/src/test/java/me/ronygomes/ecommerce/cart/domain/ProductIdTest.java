package me.ronygomes.ecommerce.cart.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductIdTest {

    @Test
    void wrapsGivenUuid() {
        UUID uuid = UUID.randomUUID();
        ProductId id = new ProductId(uuid);

        assertThat(id.value()).isEqualTo(uuid);
        assertThat(id.toString()).isEqualTo(uuid.toString());
    }

    @Test
    void nullValue_throws() {
        assertThatThrownBy(() -> new ProductId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void fromString_roundTrips() {
        UUID uuid = UUID.randomUUID();
        assertThat(ProductId.fromString(uuid.toString()).value()).isEqualTo(uuid);
    }

    @Test
    void fromString_invalidUuid_throws() {
        assertThatThrownBy(() -> ProductId.fromString("nope"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
