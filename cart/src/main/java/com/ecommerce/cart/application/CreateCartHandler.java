package com.ecommerce.cart.application;

import com.ecommerce.core.application.ICommandHandler;
import com.ecommerce.cart.domain.*;
import com.ecommerce.cart.infrastructure.ICartRepository;
import com.google.inject.Inject;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CreateCartHandler implements ICommandHandler<CreateCartCommand, Void> {
    private final ICartRepository repository;

    @Inject
    public CreateCartHandler(ICartRepository repository) {
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
