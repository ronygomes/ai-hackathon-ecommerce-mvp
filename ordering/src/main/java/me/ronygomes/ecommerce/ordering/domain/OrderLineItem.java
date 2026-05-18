package me.ronygomes.ecommerce.ordering.domain;

import java.util.Objects;
import java.util.UUID;

public class OrderLineItem {
    private UUID productId;
    private String skuSnapshot;
    private String nameSnapshot;
    private double unitPriceSnapshot;
    private int quantity;
    private double lineTotal;

    // Required for Jackson — populates fields via reflection (see BaseMongoRepository.aggregateMapper).
    private OrderLineItem() {
    }

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
