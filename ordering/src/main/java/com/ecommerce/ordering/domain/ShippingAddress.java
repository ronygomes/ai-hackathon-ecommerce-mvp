package com.ecommerce.ordering.domain;

import java.util.Objects;

public record ShippingAddress(String line1, String city, String postalCode, String country) {
    public ShippingAddress {
        Objects.requireNonNull(line1);
        Objects.requireNonNull(city);
        if (line1.isBlank() || city.isBlank()) {
            throw new IllegalArgumentException("Shipping address line1 and city are required");
        }
    }
}
