package me.ronygomes.ecommerce.ordering.application;

import com.google.inject.Inject;
import me.ronygomes.ecommerce.core.application.CommandHandler;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
import me.ronygomes.ecommerce.ordering.domain.*;
import me.ronygomes.ecommerce.ordering.infrastructure.OrderRepository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PlaceOrderHandler implements CommandHandler<PlaceOrderCommand, UUID> {
    private final OrderRepository repository;
    private final MessageBus messageBus;

    @Inject
    public PlaceOrderHandler(OrderRepository repository, MessageBus messageBus) {
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
