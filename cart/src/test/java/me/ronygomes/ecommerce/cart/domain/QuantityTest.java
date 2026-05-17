package me.ronygomes.ecommerce.cart.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuantityTest {

    @Test
    void positive_isAccepted() {
        assertThat(new Quantity(1).value()).isEqualTo(1);
        assertThat(new Quantity(7).value()).isEqualTo(7);
    }

    @Test
    void zero_throws() {
        assertThatThrownBy(() -> new Quantity(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be >= 1");
    }

    @Test
    void negative_throws() {
        assertThatThrownBy(() -> new Quantity(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void add_addsDelta() {
        assertThat(new Quantity(3).add(4).value()).isEqualTo(7);
    }

    @Test
    void add_resultingInZero_throws() {
        assertThatThrownBy(() -> new Quantity(1).add(-5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
