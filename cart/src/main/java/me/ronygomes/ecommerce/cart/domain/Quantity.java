package me.ronygomes.ecommerce.cart.domain;

public record Quantity(int value) {
    public Quantity {
        if (value < 1) {
            throw new IllegalArgumentException("Quantity must be >= 1");
        }
    }

    public Quantity add(int delta) {
        return new Quantity(this.value + delta);
    }
}
