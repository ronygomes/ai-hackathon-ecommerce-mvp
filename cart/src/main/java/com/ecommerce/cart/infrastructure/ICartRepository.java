package com.ecommerce.cart.infrastructure;

import com.ecommerce.core.infrastructure.IRepository;
import com.ecommerce.cart.domain.ShoppingCart;
import com.ecommerce.cart.domain.CartId;
import com.ecommerce.cart.domain.GuestToken;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ICartRepository extends IRepository<ShoppingCart, CartId> {
    CompletableFuture<Optional<ShoppingCart>> getByGuestToken(GuestToken guestToken);
}
