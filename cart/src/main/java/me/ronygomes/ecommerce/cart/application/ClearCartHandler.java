package me.ronygomes.ecommerce.cart.application;

import com.google.inject.Inject;
import me.ronygomes.ecommerce.checkout.saga.message.command.ClearCartCommand;
import me.ronygomes.ecommerce.cart.domain.CartId;
import me.ronygomes.ecommerce.cart.domain.ShoppingCart;
import me.ronygomes.ecommerce.cart.infrastructure.CartRepository;
import me.ronygomes.ecommerce.core.application.CommandHandler;
import me.ronygomes.ecommerce.core.infrastructure.outbox.OutboxStore;

import java.util.concurrent.CompletableFuture;

public class ClearCartHandler implements CommandHandler<ClearCartCommand, Void> {
    private final CartRepository repository;
    private final OutboxStore outboxStore;

    @Inject
    public ClearCartHandler(CartRepository repository, OutboxStore outboxStore) {
        this.repository = repository;
        this.outboxStore = outboxStore;
    }

    @Override
    public CompletableFuture<Void> handle(ClearCartCommand command) {
        return repository.getById(CartId.fromGuestToken(command.guestToken()))
                .thenCompose(cartOpt -> {
                    if (cartOpt.isPresent()) {
                        ShoppingCart cart = cartOpt.get();
                        cart.clear(command.correlationId(), command.causationId());
                        return repository.save(cart)
                                .thenAccept(v -> {
                                    outboxStore.append(cart.getId().toString(), cart.getUncommittedEvents());
                                    cart.clearUncommittedEvents();
                                });
                    }
                    return CompletableFuture.completedFuture(null);
                });
    }
}
