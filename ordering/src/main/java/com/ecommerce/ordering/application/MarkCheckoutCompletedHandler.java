package com.ecommerce.ordering.application;

import com.ecommerce.core.application.ICommandHandler;
import com.ecommerce.core.messaging.IMessageBus;
import com.ecommerce.ordering.domain.*;
import com.ecommerce.ordering.infrastructure.IOrderRepository;
import com.google.inject.Inject;
import java.util.concurrent.CompletableFuture;

public class MarkCheckoutCompletedHandler implements ICommandHandler<MarkCheckoutCompletedCommand, Void> {
    private final IOrderRepository repository;
    private final IMessageBus messageBus;

    @Inject
    public MarkCheckoutCompletedHandler(IOrderRepository repository, IMessageBus messageBus) {
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
