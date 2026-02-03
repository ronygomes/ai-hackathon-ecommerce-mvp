package me.ronygomes.ecommerce.cart.application;

import me.ronygomes.ecommerce.core.application.CommandHandler;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
import me.ronygomes.ecommerce.cart.domain.GuestToken;
import me.ronygomes.ecommerce.cart.domain.ProductId;
import me.ronygomes.ecommerce.cart.domain.Quantity;
import me.ronygomes.ecommerce.cart.domain.ShoppingCart;
import me.ronygomes.ecommerce.cart.infrastructure.CartRepository;
import com.google.inject.Inject;
import java.util.concurrent.CompletableFuture;

public class UpdateCartItemQtyHandler implements CommandHandler<UpdateCartItemQtyCommand, Void> {
    private final CartRepository repository;
    private final MessageBus messageBus;

    @Inject
    public UpdateCartItemQtyHandler(CartRepository repository, MessageBus messageBus) {
        this.repository = repository;
        this.messageBus = messageBus;
    }

    @Override
    public CompletableFuture<Void> handle(UpdateCartItemQtyCommand command) {
        GuestToken guestToken = new GuestToken(command.guestToken());
        ProductId pid = new ProductId(command.productId());
        Quantity qty = new Quantity(command.qty());

        return repository.getByGuestToken(guestToken)
                .thenCompose(opt -> {
                    if (opt.isEmpty())
                        return CompletableFuture.completedFuture(null);

                    ShoppingCart cart = opt.get();
                    cart.changeQuantity(pid, qty);

                    return repository.save(cart)
                            .thenCompose(v -> messageBus.publish(cart.getUncommittedEvents()))
                            .thenRun(cart::clearUncommittedEvents);
                });
    }
}
