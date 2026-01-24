package com.ecommerce.cart.domain;

import java.util.Objects;

public class CartItem {
    private final ProductId productId;
    private Quantity quantity;

    public CartItem(ProductId productId, Quantity quantity) {
        this.productId = Objects.requireNonNull(productId);
        this.quantity = Objects.requireNonNull(quantity);
    }

    public ProductId getProductId() {
        return productId;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public void updateQuantity(Quantity newQty) {
        this.quantity = newQty;
    }

    public void increaseQuantity(int delta) {
        this.quantity = this.quantity.add(delta);
    }
}
