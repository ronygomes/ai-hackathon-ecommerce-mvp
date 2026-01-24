package com.ecommerce.cart.application;

import com.ecommerce.core.application.ICommandHandler;
import com.ecommerce.core.messaging.IMessageBus;
import com.ecommerce.cart.domain.*;
import com.ecommerce.cart.infrastructure.ICartRepository;
import com.google.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class GetCartSnapshotHandler implements ICommandHandler<GetCartSnapshotCommand, Void> {
    private final ICartRepository repository;
    private final IMessageBus messageBus;

    @Inject
    public GetCartSnapshotHandler(ICartRepository repository, IMessageBus messageBus) {
        this.repository = repository;
        this.messageBus = messageBus;
    }

    @Override
    public CompletableFuture<Void> handle(GetCartSnapshotCommand command) {
        return repository.getByGuestToken(new GuestToken(command.guestToken()))
                .thenCompose(opt -> {
                    if (opt.isEmpty())
                        throw new RuntimeException("Cart not found for: " + command.guestToken());

                    ShoppingCart cart = opt.get();
                    List<CartSnapshotProvided.CartItemSnapshot> snapshots = cart.getItems().stream()
                            .map(i -> new CartSnapshotProvided.CartItemSnapshot(i.getProductId().value(),
                                    i.getQuantity().value()))
                            .collect(Collectors.toList());

                    return messageBus.publish(List.of(new CartSnapshotProvided(command.guestToken(), snapshots)));
                });
    }
}
