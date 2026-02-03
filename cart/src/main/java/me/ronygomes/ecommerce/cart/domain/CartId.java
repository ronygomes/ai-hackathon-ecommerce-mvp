package me.ronygomes.ecommerce.cart.domain;

import java.util.Objects;
import java.util.UUID;

public record CartId(UUID value) {
    public CartId {
        Objects.requireNonNull(value);
    }

    public static CartId generate() {
        return new CartId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
