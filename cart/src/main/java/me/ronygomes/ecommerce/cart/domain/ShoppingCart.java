package me.ronygomes.ecommerce.cart.domain;

import me.ronygomes.ecommerce.checkout.saga.message.event.CartCleared;
import me.ronygomes.ecommerce.core.domain.BaseAggregate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ShoppingCart extends BaseAggregate<CartId> {
    private GuestToken guestToken;
    private List<CartItem> items;

    // Required for Jackson — populates fields via reflection (see BaseMongoRepository.aggregateMapper).
    private ShoppingCart() {
        this.items = new ArrayList<>();
    }

    private ShoppingCart(CartId id, GuestToken guestToken) {
        this.id = id;
        this.guestToken = guestToken;
        this.items = new ArrayList<>();
    }

    public static ShoppingCart create(CartId id, GuestToken guestToken) {
        ShoppingCart cart = new ShoppingCart(id, guestToken);
        cart.addEvent(new CartCreated(id.value(), guestToken.value()));
        return cart;
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
            int oldQty = existing.get().getQuantity().value();
            existing.get().addQuantity(quantity);
            int newQty = existing.get().getQuantity().value();
            addEvent(new CartItemQuantityUpdated(id.value(), productId.value(), oldQty, newQty));
        } else {
            items.add(new CartItem(productId, quantity));
            addEvent(new CartItemAdded(id.value(), productId.value(), quantity.value()));
        }
    }

    public void removeItem(ProductId productId) {
        boolean removed = items.removeIf(i -> i.getProductId().equals(productId));
        if (removed) {
            addEvent(new CartItemRemoved(id.value(), productId.value()));
        }
    }

    public void updateQuantity(ProductId productId, Quantity quantity) {
        findItem(productId).ifPresent(i -> {
            int oldQty = i.getQuantity().value();
            items.remove(i);
            items.add(new CartItem(productId, quantity));
            addEvent(new CartItemQuantityUpdated(id.value(), productId.value(), oldQty, quantity.value()));
        });
    }

    public void changeQuantity(ProductId productId, Quantity quantity) {
        findItem(productId).ifPresent(i -> {
            int oldQty = i.getQuantity().value();
            i.updateQuantity(quantity);
            addEvent(new CartItemQuantityUpdated(id.value(), productId.value(), oldQty, quantity.value()));
        });
    }

    public void clear(UUID correlationId, String causationId) {
        this.items.clear();
        addEvent(new CartCleared(this.id.value().toString(), correlationId, causationId));
    }

    private Optional<CartItem> findItem(ProductId productId) {
        return items.stream().filter(i -> i.getProductId().equals(productId)).findFirst();
    }
}
