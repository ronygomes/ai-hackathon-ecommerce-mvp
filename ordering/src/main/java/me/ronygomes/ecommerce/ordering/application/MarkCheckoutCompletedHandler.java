package me.ronygomes.ecommerce.ordering.application;

import me.ronygomes.ecommerce.core.application.CommandHandler;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
import me.ronygomes.ecommerce.ordering.domain.Order;
import me.ronygomes.ecommerce.ordering.domain.OrderId;
import me.ronygomes.ecommerce.ordering.infrastructure.OrderRepository;
import me.rongyomes.ecommerce.checkout.saga.message.command.MarkCheckoutCompletedCommand;
import com.google.inject.Inject;
import java.util.concurrent.CompletableFuture;

public class MarkCheckoutCompletedHandler implements CommandHandler<MarkCheckoutCompletedCommand, Void> {
    private final OrderRepository repository;
    private final MessageBus messageBus;

    @Inject
    public MarkCheckoutCompletedHandler(OrderRepository repository, MessageBus messageBus) {
        this.repository = repository;
        this.messageBus = messageBus;
    }

    @Override
    public CompletableFuture<Void> handle(MarkCheckoutCompletedCommand command) {
        return repository.getById(new OrderId(command.orderId()))
                .thenCompose(opt -> {
                    if (opt.isEmpty())
                        throw new RuntimeException("Order not found: " + command.orderId());
                    Order order = opt.get();
                    order.finalizeCreated();
                    return repository.save(order)
                            .thenCompose(v -> messageBus.publish(order.getUncommittedEvents()))
                            .thenRun(order::clearUncommittedEvents);
                });
    }
}
