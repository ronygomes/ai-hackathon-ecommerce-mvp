package me.ronygomes.ecommerce.cart.application;

import com.google.inject.Inject;
import me.ronygomes.ecommerce.cart.domain.GuestToken;
import me.ronygomes.ecommerce.cart.domain.ProductId;
import me.ronygomes.ecommerce.cart.domain.ShoppingCart;
import me.ronygomes.ecommerce.cart.infrastructure.CartRepository;
import me.ronygomes.ecommerce.core.application.CommandHandler;
import me.ronygomes.ecommerce.core.infrastructure.outbox.OutboxStore;

import java.util.concurrent.CompletableFuture;

public class RemoveCartItemHandler implements CommandHandler<RemoveCartItemCommand, Void> {
    private final CartRepository repository;
    private final OutboxStore outboxStore;

    @Inject
    public RemoveCartItemHandler(CartRepository repository, OutboxStore outboxStore) {
        this.repository = repository;
        this.outboxStore = outboxStore;
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
                            .thenAccept(v -> {
                                outboxStore.append(cart.getId().toString(), cart.getUncommittedEvents());
                                cart.clearUncommittedEvents();
                            });
                });
    }
}
