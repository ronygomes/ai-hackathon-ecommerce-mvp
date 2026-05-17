package me.ronygomes.ecommerce.productcatalog.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkuTest {

    @Test
    void nonBlankValue_isAccepted() {
        assertThat(new Sku("SKU-1").value()).isEqualTo("SKU-1");
    }

    @Test
    void nullValue_throws() {
        assertThatThrownBy(() -> new Sku(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SKU cannot be empty");
    }

    @Test
    void blankValue_throws() {
        assertThatThrownBy(() -> new Sku("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SKU cannot be empty");
    }
}
