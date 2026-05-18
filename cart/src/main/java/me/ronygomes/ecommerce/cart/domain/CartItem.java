package me.ronygomes.ecommerce.cart.domain;

import java.util.Objects;

public class CartItem {
    private ProductId productId;
    private Quantity quantity;

    // Required for Jackson — populates fields via reflection (see BaseMongoRepository.aggregateMapper).
    private CartItem() {
    }

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

    public void addQuantity(Quantity quantity) {
        this.quantity = this.quantity.add(quantity.value());
    }

    public void updateQuantity(Quantity newQty) {
        this.quantity = newQty;
    }

    public void increaseQuantity(int delta) {
        this.quantity = this.quantity.add(delta);
    }
}
