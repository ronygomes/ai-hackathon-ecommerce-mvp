package me.ronygomes.ecommerce.inventory.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdjustmentReasonTest {

    @Test
    void wrapsAnyValue() {
        assertThat(new AdjustmentReason("restock").value()).isEqualTo("restock");
        assertThat(new AdjustmentReason(null).value()).isNull();
    }

    @Test
    void none_returnsEmptyStringReason() {
        assertThat(AdjustmentReason.none().value()).isEmpty();
    }
}
