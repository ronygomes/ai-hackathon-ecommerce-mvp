package me.ronygomes.ecommerce.cart.domain;

import java.util.Objects;

public record GuestToken(String value) {
    public GuestToken {
        Objects.requireNonNull(value);
        if (value.isBlank()) {
            throw new IllegalArgumentException("Guest token cannot be empty");
        }
    }
}
