package me.ronygomes.ecommerce.productcatalog.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PriceTest {

    @Test
    void positiveValue_isAccepted() {
        assertThat(new Price(99.99).value()).isEqualTo(99.99);
    }

    @Test
    void zero_isAccepted() {
        assertThat(new Price(0.0).value()).isZero();
    }

    @Test
    void negativeValue_throws() {
        assertThatThrownBy(() -> new Price(-0.01))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Price cannot be negative");
    }
}
