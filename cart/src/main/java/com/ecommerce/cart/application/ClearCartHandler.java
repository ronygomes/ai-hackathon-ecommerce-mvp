package com.ecommerce.cart.application;

import com.ecommerce.core.application.ICommandHandler;
import com.ecommerce.core.messaging.IMessageBus;
import com.ecommerce.cart.domain.*;
import com.ecommerce.cart.infrastructure.ICartRepository;
import com.ecommerce.checkout.saga.messages.commands.ClearCartCommand;
import com.ecommerce.checkout.saga.messages.events.CartCleared;
import com.google.inject.Inject;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ClearCartHandler implements ICommandHandler<ClearCartCommand, Void> {
    private final ICartRepository repository;
    private final IMessageBus messageBus;

    @Inject
    public ClearCartHandler(ICartRepository repository, IMessageBus messageBus) {
        this.repository = repository;
        this.messageBus = messageBus;
    }

    @Override
    public CompletableFuture<Void> handle(ClearCartCommand command) {
        return repository.getById(new CartId(UUID.fromString(command.guestToken())))
                .thenCompose(cartOpt -> {
                    if (cartOpt.isPresent()) {
                        ShoppingCart cart = cartOpt.get();
                        cart.clear();
                        return repository.save(cart)
                                .thenCompose(v -> messageBus.publish(List.of(new CartCleared(command.guestToken()))));
                    }
                    return CompletableFuture.completedFuture(null);
                });
    }
}
