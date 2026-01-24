package com.ecommerce.ordering.application;

import com.ecommerce.core.application.ICommandHandler;
import com.ecommerce.core.messaging.IMessageBus;
import com.ecommerce.ordering.domain.*;
import com.ecommerce.ordering.infrastructure.IOrderRepository;
import com.google.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CreateOrderHandler implements ICommandHandler<CreateOrderCommand, Void> {
    private final IOrderRepository repository;
    private final IMessageBus messageBus;

    @Inject
    public CreateOrderHandler(IOrderRepository repository, IMessageBus messageBus) {
        this.repository = repository;
        this.messageBus = messageBus;
    }

    @Override
    public CompletableFuture<Void> handle(CreateOrderCommand command) {
        List<OrderLineItem> lineItems = command.items().stream()
                .map(i -> new OrderLineItem(i.productId(), i.sku(), i.name(), i.unitPrice(), i.qty()))
                .collect(Collectors.toList());

        Order order = Order.place(
                new OrderId(command.orderId()),
                new GuestToken(command.guestToken()),
                command.customerInfo(),
                command.address(),
                lineItems,
                IdempotencyKey.fromString(command.idempotencyKey()));

        return repository.save(order)
                .thenCompose(v -> messageBus.publish(order.getUncommittedEvents()))
                .thenRun(order::clearUncommittedEvents);
    }
}
