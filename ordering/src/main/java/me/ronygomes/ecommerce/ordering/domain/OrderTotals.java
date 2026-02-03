package me.ronygomes.ecommerce.ordering.domain;

import java.util.List;

public record OrderTotals(double subtotal, double shippingFee, double total) {
    public OrderTotals {
        if (subtotal < 0 || shippingFee < 0 || total < 0) {
            throw new IllegalArgumentException("Totals cannot be negative");
        }
    }

    public static OrderTotals calculate(double subtotal, double shippingFee) {
        return new OrderTotals(subtotal, shippingFee, subtotal + shippingFee);
    }

    public static OrderTotals calculate(List<OrderLineItem> items) {
        double subtotal = items.stream().mapToDouble(i -> i.getUnitPriceSnapshot() * i.getQuantity()).sum();
        double shipping = 10.0; // Flat fee for MVP
        return new OrderTotals(subtotal, shipping, subtotal + shipping);
    }
}
