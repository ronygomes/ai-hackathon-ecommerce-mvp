package me.ronygomes.ecommerce.cart.infrastructure;

import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.cart.domain.ShoppingCart;
import me.ronygomes.ecommerce.cart.domain.CartId;
import me.ronygomes.ecommerce.cart.domain.GuestToken;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface CartRepository extends Repository<ShoppingCart, CartId> {
    CompletableFuture<Optional<ShoppingCart>> getByGuestToken(GuestToken guestToken);
}
