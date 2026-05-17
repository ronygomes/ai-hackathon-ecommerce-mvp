package me.ronygomes.ecommerce.ordering.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyKeyTest {

    @Test
    void wrapsGivenUuid() {
        UUID uuid = UUID.randomUUID();
        IdempotencyKey key = new IdempotencyKey(uuid);

        assertThat(key.value()).isEqualTo(uuid);
        assertThat(key.toString()).isEqualTo(uuid.toString());
    }

    @Test
    void nullValue_throws() {
        assertThatThrownBy(() -> new IdempotencyKey(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void fromString_roundTrips() {
        UUID uuid = UUID.randomUUID();

        assertThat(IdempotencyKey.fromString(uuid.toString()).value()).isEqualTo(uuid);
    }

    @Test
    void fromString_invalidUuid_throws() {
        assertThatThrownBy(() -> IdempotencyKey.fromString("nope"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
