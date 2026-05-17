package me.ronygomes.ecommerce.productcatalog.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductDescriptionTest {

    @Test
    void acceptsArbitraryValueIncludingNull() {
        assertThat(new ProductDescription(null).value()).isNull();
        assertThat(new ProductDescription("anything goes").value()).isEqualTo("anything goes");
    }
}
