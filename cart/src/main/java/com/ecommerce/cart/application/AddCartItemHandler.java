package com.ecommerce.cart.application;

import com.ecommerce.core.application.ICommandHandler;
import com.ecommerce.core.messaging.IMessageBus;
import com.ecommerce.cart.domain.*;
import com.ecommerce.cart.infrastructure.ICartRepository;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AddCartItemHandler implements ICommandHandler<AddCartItemCommand, Void> {
    private final ICartRepository repository;
    private final IMessageBus messageBus;

    @Inject
    public AddCartItemHandler(ICartRepository repository, IMessageBus messageBus) {
        this.repository = repository;
        this.messageBus = messageBus;
    }

    @Override
    public CompletableFuture<Void> handle(AddCartItemCommand command) {
        return repository.getById(new CartId(UUID.fromString(command.guestToken())))
                .thenCompose(opt -> {
                    ShoppingCart cart = opt.orElseGet(() -> ShoppingCart.create(
                            new CartId(UUID.fromString(command.guestToken())),
                            new GuestToken(command.guestToken())));

                    cart.addItem(ProductId.fromString(command.productId().toString()), new Quantity(command.qty()));
                    return repository.save(cart);
                })
                .thenCompose(v -> CompletableFuture.completedFuture(null));
    }
}
