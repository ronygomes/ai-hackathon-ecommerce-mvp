package com.ecommerce.cart.domain;

import com.ecommerce.core.domain.BaseAggregate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ShoppingCart extends BaseAggregate<CartId> {
    private GuestToken guestToken;
    private List<CartItem> items = new ArrayList<>();

    public ShoppingCart() {
        // Required for Jackson
    }

    private ShoppingCart(CartId id, GuestToken guestToken) {
        this.id = id;
        this.guestToken = guestToken;
    }

    public static ShoppingCart create(GuestToken guestToken) {
        CartId id = CartId.generate();
        ShoppingCart cart = new ShoppingCart(id, guestToken);
        cart.addEvent(new CartCreated(id.value(), guestToken.value()));
        return cart;
    }

    public void addItem(ProductId productId, Quantity quantity) {
        Optional<CartItem> existingItem = findItem(productId);
        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int oldQty = item.getQuantity().value();
            item.increaseQuantity(quantity.value());
            addEvent(new CartItemQuantityUpdated(this.id.value(), productId.value(), oldQty,
                    item.getQuantity().value()));
        } else {
            items.add(new CartItem(productId, quantity));
            addEvent(new CartItemAdded(this.id.value(), productId.value(), quantity.value()));
        }
    }

    public void increaseItem(ProductId productId, int delta) {
        findItem(productId).ifPresent(item -> {
            int oldQty = item.getQuantity().value();
            item.increaseQuantity(delta);
            addEvent(new CartItemQuantityUpdated(this.id.value(), productId.value(), oldQty,
                    item.getQuantity().value()));
        });
    }

    public void changeQuantity(ProductId productId, Quantity newQty) {
        findItem(productId).ifPresent(item -> {
            int oldQty = item.getQuantity().value();
            item.updateQuantity(newQty);
            addEvent(new CartItemQuantityUpdated(this.id.value(), productId.value(), oldQty, newQty.value()));
        });
    }

    public void removeItem(ProductId productId) {
        if (items.removeIf(item -> item.getProductId().equals(productId))) {
            addEvent(new CartItemRemoved(this.id.value(), productId.value()));
        }
    }

    public void clear() {
        items.clear();
        addEvent(new CartCleared(this.id.value()));
    }

    private Optional<CartItem> findItem(ProductId productId) {
        return items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();
    }

    public GuestToken getGuestToken() {
        return guestToken;
    }

    public List<CartItem> getItems() {
        return items;
    }
}
