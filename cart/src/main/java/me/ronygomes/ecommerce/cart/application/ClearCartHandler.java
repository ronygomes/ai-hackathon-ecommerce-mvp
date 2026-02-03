package me.ronygomes.ecommerce.cart.application;

import me.ronygomes.ecommerce.core.application.CommandHandler;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
import me.ronygomes.ecommerce.cart.domain.CartId;
import me.ronygomes.ecommerce.cart.domain.ShoppingCart;
import me.ronygomes.ecommerce.cart.infrastructure.CartRepository;
import me.rongyomes.ecommerce.checkout.saga.message.command.ClearCartCommand;
import me.rongyomes.ecommerce.checkout.saga.message.event.CartCleared;
import com.google.inject.Inject;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ClearCartHandler implements CommandHandler<ClearCartCommand, Void> {
    private final CartRepository repository;
    private final MessageBus messageBus;

    @Inject
    public ClearCartHandler(CartRepository repository, MessageBus messageBus) {
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
