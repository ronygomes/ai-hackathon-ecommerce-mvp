package com.ecommerce.cart.application;

import com.ecommerce.core.application.ICommandHandler;
import com.ecommerce.core.messaging.IMessageBus;
import com.ecommerce.cart.domain.*;
import com.ecommerce.cart.infrastructure.ICartRepository;
import com.google.inject.Inject;
import java.util.concurrent.CompletableFuture;

public class CreateCartHandler implements ICommandHandler<CreateCartCommand, Void> {
    private final ICartRepository repository;
    private final IMessageBus messageBus;

    @Inject
    public CreateCartHandler(ICartRepository repository, IMessageBus messageBus) {
        this.repository = repository;
        this.messageBus = messageBus;
    }

    @Override
    public CompletableFuture<Void> handle(CreateCartCommand command) {
        GuestToken guestToken = new GuestToken(command.guestToken());
        return repository.getByGuestToken(guestToken)
                .thenCompose(opt -> {
                    if (opt.isPresent()) {
                        return CompletableFuture.completedFuture(null);
                    }
                    ShoppingCart cart = ShoppingCart.create(guestToken);
                    return repository.save(cart)
                            .thenCompose(v -> messageBus.publish(cart.getUncommittedEvents()))
                            .thenRun(cart::clearUncommittedEvents);
                });
    }
}
