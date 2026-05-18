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

    @Test
    void fromGuestToken_isDeterministic() {
        CartId first = CartId.fromGuestToken("guest-abc");
        CartId second = CartId.fromGuestToken("guest-abc");

        assertThat(first).isEqualTo(second);
    }

    @Test
    void fromGuestToken_acceptsNonUuidStrings() {
        // Pre-fix, handlers parsed guestToken via UUID.fromString and exploded on
        // non-UUID input. fromGuestToken derives a UUID via name-hashing so any
        // non-null string is valid.
        CartId id = CartId.fromGuestToken("not-a-uuid");

        assertThat(id.value()).isNotNull();
    }

    @Test
    void fromGuestToken_nullThrows() {
        assertThatThrownBy(() -> CartId.fromGuestToken(null))
                .isInstanceOf(NullPointerException.class);
    }
}
