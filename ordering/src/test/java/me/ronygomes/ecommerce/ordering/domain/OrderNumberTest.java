package me.ronygomes.ecommerce.ordering.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderNumberTest {

    @Test
    void wrapsExplicitValue() {
        assertThat(new OrderNumber("ORD-1").value()).isEqualTo("ORD-1");
    }

    @Test
    void generate_producesOrdPrefixedValue() {
        OrderNumber number = OrderNumber.generate();

        assertThat(number.value()).startsWith("ORD-").matches("ORD-\\d{8}-\\d{4}");
    }
}
