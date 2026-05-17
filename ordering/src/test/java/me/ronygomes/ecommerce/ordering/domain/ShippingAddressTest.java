package me.ronygomes.ecommerce.ordering.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShippingAddressTest {

    @Test
    void validInput_isAccepted() {
        ShippingAddress addr = new ShippingAddress("1 Main St", "Anytown", "12345", "USA");

        assertThat(addr.line1()).isEqualTo("1 Main St");
        assertThat(addr.city()).isEqualTo("Anytown");
        assertThat(addr.postalCode()).isEqualTo("12345");
        assertThat(addr.country()).isEqualTo("USA");
    }

    @Test
    void nullLine1_throws() {
        assertThatThrownBy(() -> new ShippingAddress(null, "City", "x", "US"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullCity_throws() {
        assertThatThrownBy(() -> new ShippingAddress("L1", null, "x", "US"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void blankLine1_throws() {
        assertThatThrownBy(() -> new ShippingAddress(" ", "City", "x", "US"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("line1 and city are required");
    }

    @Test
    void blankCity_throws() {
        assertThatThrownBy(() -> new ShippingAddress("L1", " ", "x", "US"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
