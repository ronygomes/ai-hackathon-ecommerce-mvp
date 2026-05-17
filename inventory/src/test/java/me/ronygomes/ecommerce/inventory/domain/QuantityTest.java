package me.ronygomes.ecommerce.inventory.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuantityTest {

    @Test
    void zero_isAccepted() {
        assertThat(new Quantity(0).value()).isZero();
    }

    @Test
    void positive_isAccepted() {
        assertThat(new Quantity(10).value()).isEqualTo(10);
    }

    @Test
    void negative_throws() {
        assertThatThrownBy(() -> new Quantity(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity cannot be negative");
    }

    @Test
    void add_sumsValues() {
        assertThat(new Quantity(3).add(new Quantity(4)).value()).isEqualTo(7);
    }

    @Test
    void subtract_returnsDifference() {
        assertThat(new Quantity(10).subtract(new Quantity(4)).value()).isEqualTo(6);
    }

    @Test
    void subtract_underflow_throws() {
        assertThatThrownBy(() -> new Quantity(2).subtract(new Quantity(5)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isGreaterThanOrEqual_handlesEqualAndGreater() {
        assertThat(new Quantity(5).isGreaterThanOrEqual(new Quantity(5))).isTrue();
        assertThat(new Quantity(5).isGreaterThanOrEqual(new Quantity(3))).isTrue();
        assertThat(new Quantity(3).isGreaterThanOrEqual(new Quantity(5))).isFalse();
    }
}
