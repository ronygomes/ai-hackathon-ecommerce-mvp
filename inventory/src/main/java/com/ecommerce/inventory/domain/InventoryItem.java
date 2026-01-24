package com.ecommerce.inventory.domain;

import com.ecommerce.core.domain.BaseAggregate;
import java.util.UUID;

public class InventoryItem extends BaseAggregate<ProductId> {
    private Quantity quantity;

    public InventoryItem() {
        // Required for Jackson
    }

    private InventoryItem(ProductId productId, Quantity initialQuantity) {
        this.id = productId;
        this.quantity = initialQuantity;
    }

    public static InventoryItem create(ProductId productId, Quantity initialQuantity) {
        InventoryItem item = new InventoryItem(productId, initialQuantity);
        item.addEvent(new StockItemCreated(productId.value(), initialQuantity.value()));
        return item;
    }

    public void setStock(Quantity newQty, AdjustmentReason reason) {
        Quantity oldQty = this.quantity;
        this.quantity = newQty;
        this.addEvent(new StockSet(this.id.value(), oldQty.value(), newQty.value(), reason.value(), "admin"));
    }

    public void deductForOrder(UUID orderId, Quantity qty) {
        if (!this.quantity.isGreaterThanOrEqual(qty)) {
            this.addEvent(new StockDeductionRejected(orderId, this.id.value(), qty.value(), this.quantity.value(),
                    "Insufficient stock"));
            return;
        }

        Quantity oldQty = this.quantity;
        this.quantity = this.quantity.subtract(qty);
        this.addEvent(new StockDeductedForOrder(orderId, this.id.value(), qty.value(), oldQty.value(),
                this.quantity.value()));
    }

    public boolean isAvailable(Quantity requestedQty) {
        return this.quantity.isGreaterThanOrEqual(requestedQty);
    }

    public Quantity getQuantity() {
        return quantity;
    }
}
