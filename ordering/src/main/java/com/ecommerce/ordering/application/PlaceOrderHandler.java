package com.ecommerce.ordering.application;

import com.ecommerce.core.application.ICommandHandler;
import com.ecommerce.core.messaging.IMessageBus;
import com.ecommerce.ordering.domain.*;
import com.ecommerce.ordering.infrastructure.IOrderRepository;
import com.google.inject.Inject;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PlaceOrderHandler implements ICommandHandler<PlaceOrderCommand, UUID> {
    private final IOrderRepository repository;
    private final IMessageBus messageBus;

    @Inject
    public PlaceOrderHandler(IOrderRepository repository, IMessageBus messageBus) {
        this.repository = repository;
        this.messageBus = messageBus;
    }

    @Override
    public CompletableFuture<UUID> handle(PlaceOrderCommand command) {
        IdempotencyKey key = IdempotencyKey.fromString(command.idempotencyKey());

        return repository.getByIdempotencyKey(key)
                .thenCompose(existingOpt -> {
                    if (existingOpt.isPresent()) {
                        return CompletableFuture.completedFuture(existingOpt.get().getId().value());
                    }

                    List<OrderLineItem> lineItems = command.items().stream()
                            .map(i -> new OrderLineItem(i.productId(), i.sku(), i.name(), i.unitPrice(), i.qty()))
                            .collect(Collectors.toList());

                    OrderId orderId = OrderId.generate();
                    Order order = Order.place(
                            orderId,
                            new GuestToken(command.guestToken()),
                            command.customerInfo(),
                            command.address(),
                            lineItems,
                            key);

                    return repository.save(order)
                            .thenCompose(v -> messageBus.publish(order.getUncommittedEvents()))
                            .thenRun(order::clearUncommittedEvents)
                            .thenApply(v -> orderId.value());
                });
    }
}
