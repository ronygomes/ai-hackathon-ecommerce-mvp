package me.ronygomes.ecommerce.ordering.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderLineItemTest {

    @Test
    void construct_storesAllFieldsAndComputesLineTotal() {
        UUID productId = UUID.randomUUID();

        OrderLineItem item = new OrderLineItem(productId, "SKU-1", "Widget", 9.99, 3);

        assertThat(item.getProductId()).isEqualTo(productId);
        assertThat(item.getSkuSnapshot()).isEqualTo("SKU-1");
        assertThat(item.getNameSnapshot()).isEqualTo("Widget");
        assertThat(item.getUnitPriceSnapshot()).isEqualTo(9.99);
        assertThat(item.getQuantity()).isEqualTo(3);
        assertThat(item.getLineTotal()).isEqualTo(29.97);
    }

    @Test
    void nullProductId_throws() {
        assertThatThrownBy(() -> new OrderLineItem(null, "S", "N", 1.0, 1))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullSku_throws() {
        assertThatThrownBy(() -> new OrderLineItem(UUID.randomUUID(), null, "N", 1.0, 1))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void zeroQuantity_throws() {
        assertThatThrownBy(() -> new OrderLineItem(UUID.randomUUID(), "S", "N", 1.0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be >= 1");
    }
}
