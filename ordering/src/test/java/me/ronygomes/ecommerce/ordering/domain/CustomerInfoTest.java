package me.ronygomes.ecommerce.ordering.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerInfoTest {

    @Test
    void validInput_isAccepted() {
        CustomerInfo info = new CustomerInfo("Jane", "+12345", "jane@example.com");

        assertThat(info.name()).isEqualTo("Jane");
        assertThat(info.phone()).isEqualTo("+12345");
        assertThat(info.email()).isEqualTo("jane@example.com");
    }

    @Test
    void nullName_throws() {
        assertThatThrownBy(() -> new CustomerInfo(null, "+1", "e@x"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullPhone_throws() {
        assertThatThrownBy(() -> new CustomerInfo("Ok", null, "e@x"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void singleCharacterName_throws() {
        assertThatThrownBy(() -> new CustomerInfo("A", "+1", "e@x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 2 characters");
    }

    @Test
    void nullEmail_isAllowed() {
        assertThat(new CustomerInfo("Jane", "+1", null).email()).isNull();
    }
}
