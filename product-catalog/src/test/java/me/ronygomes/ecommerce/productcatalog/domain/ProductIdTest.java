package me.ronygomes.ecommerce.productcatalog.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductIdTest {

    @Test
    void wrapsAGivenUuid() {
        UUID uuid = UUID.randomUUID();
        ProductId id = new ProductId(uuid);

        assertThat(id.value()).isEqualTo(uuid);
        assertThat(id.toString()).isEqualTo(uuid.toString());
    }

    @Test
    void nullUuid_throws() {
        assertThatThrownBy(() -> new ProductId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ProductId cannot be null");
    }

    @Test
    void generate_producesUniqueValues() {
        ProductId a = ProductId.generate();
        ProductId b = ProductId.generate();

        assertThat(a.value()).isNotNull();
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void fromString_roundTrips() {
        UUID uuid = UUID.randomUUID();

        ProductId id = ProductId.fromString(uuid.toString());

        assertThat(id.value()).isEqualTo(uuid);
    }

    @Test
    void fromString_withInvalidUuid_throws() {
        assertThatThrownBy(() -> ProductId.fromString("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
