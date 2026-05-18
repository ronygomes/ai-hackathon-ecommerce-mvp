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

    /**
     * Derives a deterministic CartId from a guestToken string via name-based UUID
     * hashing. The same input always produces the same id, so handlers can look up
     * a cart by guestToken without storing a separate (guestToken → cartId) index.
     * Works for any non-null string — guestToken is not required to be a UUID.
     */
    public static CartId fromGuestToken(String guestToken) {
        Objects.requireNonNull(guestToken);
        return new CartId(UUID.nameUUIDFromBytes(guestToken.getBytes()));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
