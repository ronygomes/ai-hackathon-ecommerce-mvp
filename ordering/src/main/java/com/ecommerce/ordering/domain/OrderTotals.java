package com.ecommerce.ordering.domain;

public record OrderTotals(double subtotal, double shippingFee, double total) {
    public OrderTotals {
        if (subtotal < 0 || shippingFee < 0 || total < 0) {
            throw new IllegalArgumentException("Totals cannot be negative");
        }
    }

    public static OrderTotals calculate(double subtotal, double shippingFee) {
        return new OrderTotals(subtotal, shippingFee, subtotal + shippingFee);
    }
}
