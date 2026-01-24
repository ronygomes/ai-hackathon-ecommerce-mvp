package com.ecommerce.cart.application;

import com.ecommerce.core.application.ICommandHandler;
import com.ecommerce.core.messaging.IMessageBus;
import com.ecommerce.cart.domain.*;
import com.ecommerce.cart.infrastructure.ICartRepository;
import com.google.inject.Inject;
import java.util.concurrent.CompletableFuture;

public class RemoveCartItemHandler implements ICommandHandler<RemoveCartItemCommand, Void> {
    private final ICartRepository repository;
    private final IMessageBus messageBus;

    @Inject
    public RemoveCartItemHandler(ICartRepository repository, IMessageBus messageBus) {
        this.repository = repository;
        this.messageBus = messageBus;
    }

    @Override
    public CompletableFuture<Void> handle(RemoveCartItemCommand command) {
        GuestToken guestToken = new GuestToken(command.guestToken());
        ProductId pid = new ProductId(command.productId());

        return repository.getByGuestToken(guestToken)
                .thenCompose(opt -> {
                    if (opt.isEmpty())
                        return CompletableFuture.completedFuture(null);

                    ShoppingCart cart = opt.get();
                    cart.removeItem(pid);

                    return repository.save(cart)
                            .thenCompose(v -> messageBus.publish(cart.getUncommittedEvents()))
                            .thenRun(cart::clearUncommittedEvents);
                });
    }
}
