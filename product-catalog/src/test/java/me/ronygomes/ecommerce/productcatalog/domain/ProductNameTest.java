package me.ronygomes.ecommerce.productcatalog.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductNameTest {

    @Test
    void twoOrMoreCharacters_isAccepted() {
        assertThat(new ProductName("Ab").value()).isEqualTo("Ab");
    }

    @Test
    void singleCharacter_throws() {
        assertThatThrownBy(() -> new ProductName("A"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 2 characters");
    }

    @Test
    void nullValue_throws() {
        assertThatThrownBy(() -> new ProductName(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
