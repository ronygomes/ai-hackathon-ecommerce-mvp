package me.ronygomes.ecommerce.ordering.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTotalsTest {

    @Test
    void calculateFromSubtotalAndShipping_sumsToTotal() {
        OrderTotals totals = OrderTotals.calculate(50.0, 5.0);

        assertThat(totals.subtotal()).isEqualTo(50.0);
        assertThat(totals.shippingFee()).isEqualTo(5.0);
        assertThat(totals.total()).isEqualTo(55.0);
    }

    @Test
    void calculateFromItems_usesZeroShippingFeePerMVPSpec() {
        OrderTotals totals = OrderTotals.calculate(List.of(
                new OrderLineItem(UUID.randomUUID(), "SKU-A", "A", 10.0, 2),
                new OrderLineItem(UUID.randomUUID(), "SKU-B", "B", 5.0, 1)));

        assertThat(totals.subtotal()).isEqualTo(25.0);
        assertThat(totals.shippingFee()).isEqualTo(0.0);
        assertThat(totals.total()).isEqualTo(25.0);
    }

    @Test
    void negativeSubtotal_throws() {
        assertThatThrownBy(() -> new OrderTotals(-1.0, 0.0, -1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Totals cannot be negative");
    }

    @Test
    void negativeShipping_throws() {
        assertThatThrownBy(() -> new OrderTotals(10.0, -1.0, 9.0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
