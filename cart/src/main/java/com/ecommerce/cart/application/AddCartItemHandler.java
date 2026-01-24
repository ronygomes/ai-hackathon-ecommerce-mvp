package com.ecommerce.cart.application;

import com.ecommerce.core.application.ICommandHandler;
import com.ecommerce.core.messaging.IMessageBus;
import com.ecommerce.cart.domain.*;
import com.ecommerce.cart.infrastructure.ICartRepository;
import com.google.inject.Inject;
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
        GuestToken guestToken = new GuestToken(command.guestToken());
        ProductId pid = new ProductId(command.productId());
        Quantity qty = new Quantity(command.qty());

        // Soft validation would happen here (async check against Catalog/Inventory read
        // models)
        // For MVP simplicity, we proceed with the domain operation.

        return repository.getByGuestToken(guestToken)
                .thenCompose(opt -> {
                    ShoppingCart cart = opt.orElseGet(() -> ShoppingCart.create(guestToken));
                    cart.addItem(pid, qty);

                    return repository.save(cart)
                            .thenCompose(v -> messageBus.publish(cart.getUncommittedEvents()))
                            .thenRun(cart::clearUncommittedEvents);
                });
    }
}
