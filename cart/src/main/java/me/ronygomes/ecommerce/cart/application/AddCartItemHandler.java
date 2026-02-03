package me.ronygomes.ecommerce.cart.application;

import com.google.inject.Inject;
import me.ronygomes.ecommerce.cart.domain.*;
import me.ronygomes.ecommerce.cart.infrastructure.CartRepository;
import me.ronygomes.ecommerce.core.application.CommandHandler;
import me.ronygomes.ecommerce.core.messaging.MessageBus;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AddCartItemHandler implements CommandHandler<AddCartItemCommand, Void> {
    private final CartRepository repository;
    private final MessageBus messageBus;

    @Inject
    public AddCartItemHandler(CartRepository repository, MessageBus messageBus) {
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
