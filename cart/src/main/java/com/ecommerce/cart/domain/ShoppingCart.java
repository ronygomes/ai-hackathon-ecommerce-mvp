package com.ecommerce.cart.domain;

import com.ecommerce.core.domain.BaseAggregate;
import com.ecommerce.checkout.saga.messages.events.CartCleared;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ShoppingCart extends BaseAggregate<CartId> {
    private final GuestToken guestToken;
    private final List<CartItem> items;

    private ShoppingCart(CartId id, GuestToken guestToken) {
        this.id = id;
        this.guestToken = guestToken;
        this.items = new ArrayList<>();
    }

    public static ShoppingCart create(CartId id, GuestToken guestToken) {
        return new ShoppingCart(id, guestToken);
    }

    public GuestToken getGuestToken() {
        return guestToken;
    }

    public List<CartItem> getItems() {
        return List.copyOf(items);
    }

    public void addItem(ProductId productId, Quantity quantity) {
        Optional<CartItem> existing = findItem(productId);
        if (existing.isPresent()) {
            existing.get().addQuantity(quantity);
        } else {
            items.add(new CartItem(productId, quantity));
        }
    }

    public void removeItem(ProductId productId) {
        items.removeIf(i -> i.getProductId().equals(productId));
    }

    public void updateQuantity(ProductId productId, Quantity quantity) {
        findItem(productId).ifPresent(i -> {
            items.remove(i);
            items.add(new CartItem(productId, quantity));
        });
    }

    public void changeQuantity(ProductId productId, Quantity quantity) {
        findItem(productId).ifPresent(i -> i.updateQuantity(quantity));
    }

    public void clear() {
        this.items.clear();
        addEvent(new CartCleared(this.id.value().toString()));
    }

    private Optional<CartItem> findItem(ProductId productId) {
        return items.stream().filter(i -> i.getProductId().equals(productId)).findFirst();
    }
}
