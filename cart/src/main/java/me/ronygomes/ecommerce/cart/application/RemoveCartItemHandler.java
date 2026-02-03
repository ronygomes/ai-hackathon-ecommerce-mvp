package me.ronygomes.ecommerce.cart.application;

import me.ronygomes.ecommerce.core.application.CommandHandler;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
import me.ronygomes.ecommerce.cart.domain.GuestToken;
import me.ronygomes.ecommerce.cart.domain.ProductId;
import me.ronygomes.ecommerce.cart.domain.ShoppingCart;
import me.ronygomes.ecommerce.cart.infrastructure.CartRepository;
import com.google.inject.Inject;
import java.util.concurrent.CompletableFuture;

public class RemoveCartItemHandler implements CommandHandler<RemoveCartItemCommand, Void> {
    private final CartRepository repository;
    private final MessageBus messageBus;

    @Inject
    public RemoveCartItemHandler(CartRepository repository, MessageBus messageBus) {
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
