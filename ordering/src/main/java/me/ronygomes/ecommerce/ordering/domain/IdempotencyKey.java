package me.ronygomes.ecommerce.ordering.domain;

import java.util.Objects;
import java.util.UUID;

public record IdempotencyKey(UUID value) {
    public IdempotencyKey {
        Objects.requireNonNull(value);
    }

    public static IdempotencyKey fromString(String value) {
        return new IdempotencyKey(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
