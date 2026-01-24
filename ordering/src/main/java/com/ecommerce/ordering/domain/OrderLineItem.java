package com.ecommerce.ordering.domain;

import java.util.Objects;
import java.util.UUID;

public class OrderLineItem {
    private final UUID productId;
    private final String skuSnapshot;
    private final String nameSnapshot;
    private final double unitPriceSnapshot;
    private final int quantity;
    private final double lineTotal;

    public OrderLineItem(UUID productId, String skuSnapshot, String nameSnapshot, double unitPriceSnapshot,
            int quantity) {
        this.productId = Objects.requireNonNull(productId);
        this.skuSnapshot = Objects.requireNonNull(skuSnapshot);
        this.nameSnapshot = Objects.requireNonNull(nameSnapshot);
        this.unitPriceSnapshot = unitPriceSnapshot;
        this.quantity = quantity;
        this.lineTotal = unitPriceSnapshot * quantity;

        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be >= 1");
        }
    }

    public UUID getProductId() {
        return productId;
    }

    public String getSkuSnapshot() {
        return skuSnapshot;
    }

    public String getNameSnapshot() {
        return nameSnapshot;
    }

    public double getUnitPriceSnapshot() {
        return unitPriceSnapshot;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getLineTotal() {
        return lineTotal;
    }
}
