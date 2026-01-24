package com.ecommerce.ordering.domain;

import java.util.Objects;

public record CustomerInfo(String name, String phone, String email) {
    public CustomerInfo {
        Objects.requireNonNull(name);
        Objects.requireNonNull(phone);
        if (name.length() < 2) {
            throw new IllegalArgumentException("Customer name must be at least 2 characters");
        }
    }
}
