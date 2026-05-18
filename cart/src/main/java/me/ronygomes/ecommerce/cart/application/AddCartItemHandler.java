package me.ronygomes.ecommerce.cart.application;

import com.google.inject.Inject;
import me.ronygomes.ecommerce.cart.domain.*;
import me.ronygomes.ecommerce.cart.infrastructure.CartRepository;
import me.ronygomes.ecommerce.core.application.CommandHandler;
import me.ronygomes.ecommerce.core.infrastructure.outbox.OutboxStore;

import java.util.concurrent.CompletableFuture;

public class AddCartItemHandler implements CommandHandler<AddCartItemCommand, Void> {
    private final CartRepository repository;
    private final OutboxStore outboxStore;

    @Inject
    public AddCartItemHandler(CartRepository repository, OutboxStore outboxStore) {
        this.repository = repository;
        this.outboxStore = outboxStore;
    }

    @Override
    public CompletableFuture<Void> handle(AddCartItemCommand command) {
        return repository.getById(CartId.fromGuestToken(command.guestToken()))
                .thenCompose(opt -> {
                    ShoppingCart cart = opt.orElseGet(() -> ShoppingCart.create(
                            CartId.fromGuestToken(command.guestToken()),
                            new GuestToken(command.guestToken())));

                    cart.addItem(ProductId.fromString(command.productId().toString()), new Quantity(command.qty()));
                    return repository.save(cart)
                            .thenAccept(v -> {
                                outboxStore.append(cart.getId().toString(), cart.getUncommittedEvents());
                                cart.clearUncommittedEvents();
                            });
                });
    }
}
