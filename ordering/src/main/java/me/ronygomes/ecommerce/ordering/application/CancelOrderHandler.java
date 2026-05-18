package me.ronygomes.ecommerce.ordering.application;

import com.google.inject.Inject;
import me.ronygomes.ecommerce.checkout.saga.message.command.CancelOrderCommand;
import me.ronygomes.ecommerce.core.application.CommandHandler;
import me.ronygomes.ecommerce.core.infrastructure.outbox.OutboxStore;
import me.ronygomes.ecommerce.ordering.domain.Order;
import me.ronygomes.ecommerce.ordering.domain.OrderId;
import me.ronygomes.ecommerce.ordering.infrastructure.OrderRepository;

import java.util.concurrent.CompletableFuture;

public class CancelOrderHandler implements CommandHandler<CancelOrderCommand, Void> {
    private final OrderRepository repository;
    private final OutboxStore outboxStore;

    @Inject
    public CancelOrderHandler(OrderRepository repository, OutboxStore outboxStore) {
        this.repository = repository;
        this.outboxStore = outboxStore;
    }

    @Override
    public CompletableFuture<Void> handle(CancelOrderCommand command) {
        return repository.getById(new OrderId(command.orderId()))
                .thenCompose(opt -> {
                    if (opt.isEmpty())
                        return CompletableFuture.<Void>failedFuture(
                                new RuntimeException("Order not found: " + command.orderId()));
                    Order order = opt.get();
                    order.cancel(command.reason());
                    return repository.save(order)
                            .thenAccept(v -> {
                                outboxStore.append(order.getId().toString(), order.getUncommittedEvents());
                                order.clearUncommittedEvents();
                            });
                });
    }
}
