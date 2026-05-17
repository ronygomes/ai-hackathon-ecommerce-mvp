package me.ronygomes.ecommerce.cart.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuestTokenTest {

    @Test
    void nonBlankValue_isAccepted() {
        assertThat(new GuestToken("guest-abc").value()).isEqualTo("guest-abc");
    }

    @Test
    void nullValue_throws() {
        assertThatThrownBy(() -> new GuestToken(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void blankValue_throws() {
        assertThatThrownBy(() -> new GuestToken("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Guest token cannot be empty");
    }
}
