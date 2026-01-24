package com.ecommerce.inventory.domain;

public record AdjustmentReason(String value) {
    public static AdjustmentReason none() {
        return new AdjustmentReason("");
    }
}
