package me.ronygomes.ecommerce.cart.application;

import me.ronygomes.ecommerce.core.application.CommandHandler;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
import me.ronygomes.ecommerce.cart.domain.CartId;
import me.ronygomes.ecommerce.cart.domain.GuestToken;
import me.ronygomes.ecommerce.cart.domain.ShoppingCart;
import me.ronygomes.ecommerce.cart.infrastructure.CartRepository;
import me.rongyomes.ecommerce.checkout.saga.message.command.GetCartSnapshotCommand;
import me.rongyomes.ecommerce.checkout.saga.message.event.CartSnapshotProvided;
import com.google.inject.Inject;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class GetCartSnapshotHandler implements CommandHandler<GetCartSnapshotCommand, Void> {
    private final CartRepository repository;
    private final MessageBus messageBus;

    @Inject
    public GetCartSnapshotHandler(CartRepository repository, MessageBus messageBus) {
        this.repository = repository;
        this.messageBus = messageBus;
    }

    @Override
    public CompletableFuture<Void> handle(GetCartSnapshotCommand command) {
        return repository.getById(new CartId(UUID.fromString(command.guestToken())))
                .thenCompose(cartOpt -> {
                    ShoppingCart cart = cartOpt.orElseGet(() -> ShoppingCart.create(
                            new CartId(UUID.fromString(command.guestToken())),
                            new GuestToken(command.guestToken())));

                    List<CartSnapshotProvided.CartItemSnapshot> snapshots = cart
                            .getItems().stream()
                            .map(i -> new CartSnapshotProvided.CartItemSnapshot(
                                    i.getProductId().value(),
                                    i.getQuantity().value()))
                            .collect(Collectors.toList());

                    return messageBus.publish(List.of(new CartSnapshotProvided(command.guestToken(), snapshots)));
                });
    }
}
