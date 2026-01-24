package com.ecommerce.inventory.domain;

public record Quantity(int value) {
    public Quantity {
        if (value < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
    }

    public Quantity add(Quantity other) {
        return new Quantity(this.value + other.value());
    }

    public Quantity subtract(Quantity other) {
        return new Quantity(this.value - other.value());
    }

    public boolean isGreaterThanOrEqual(Quantity other) {
        return this.value >= other.value();
    }
}
