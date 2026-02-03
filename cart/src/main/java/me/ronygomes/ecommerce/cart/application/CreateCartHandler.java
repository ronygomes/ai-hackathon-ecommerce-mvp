package me.ronygomes.ecommerce.cart.application;

import me.ronygomes.ecommerce.core.application.CommandHandler;
import me.ronygomes.ecommerce.cart.domain.CartId;
import me.ronygomes.ecommerce.cart.domain.GuestToken;
import me.ronygomes.ecommerce.cart.domain.ShoppingCart;
import me.ronygomes.ecommerce.cart.infrastructure.CartRepository;
import com.google.inject.Inject;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CreateCartHandler implements CommandHandler<CreateCartCommand, Void> {
    private final CartRepository repository;

    @Inject
    public CreateCartHandler(CartRepository repository) {
        this.repository = repository;
    }

    @Override
    public CompletableFuture<Void> handle(CreateCartCommand command) {
        GuestToken guestToken = new GuestToken(command.guestToken());
        CartId cartId = new CartId(UUID.fromString(command.guestToken()));
        ShoppingCart cart = ShoppingCart.create(cartId, guestToken);
        return repository.save(cart);
    }
}
