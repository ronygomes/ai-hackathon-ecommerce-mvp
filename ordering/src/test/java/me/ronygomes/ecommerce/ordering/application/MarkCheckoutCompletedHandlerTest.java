package me.ronygomes.ecommerce.ordering.application;

import me.rongyomes.ecommerce.checkout.saga.message.command.MarkCheckoutCompletedCommand;
import me.rongyomes.ecommerce.checkout.saga.message.event.OrderCreated;
import me.ronygomes.ecommerce.core.domain.DomainEvent;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
import me.ronygomes.ecommerce.ordering.domain.CustomerInfo;
import me.ronygomes.ecommerce.ordering.domain.GuestToken;
import me.ronygomes.ecommerce.ordering.domain.IdempotencyKey;
import me.ronygomes.ecommerce.ordering.domain.Order;
import me.ronygomes.ecommerce.ordering.domain.OrderId;
import me.ronygomes.ecommerce.ordering.domain.OrderLineItem;
import me.ronygomes.ecommerce.ordering.domain.OrderStatus;
import me.ronygomes.ecommerce.ordering.domain.ShippingAddress;
import me.ronygomes.ecommerce.ordering.infrastructure.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarkCheckoutCompletedHandlerTest {

    private final OrderRepository repository = mock(OrderRepository.class);
    private final MessageBus messageBus = mock(MessageBus.class);
    private MarkCheckoutCompletedHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MarkCheckoutCompletedHandler(repository, messageBus);
        when(repository.save(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    private static Order placedOrder() {
        return Order.place(
                OrderId.generate(),
                new GuestToken("g1"),
                new CustomerInfo("Jane", "+1", "jane@example.com"),
                new ShippingAddress("L", "C", "x", "US"),
                List.of(new OrderLineItem(UUID.randomUUID(), "S", "N", 1.0, 1)),
                new IdempotencyKey(UUID.randomUUID()));
    }

    @Test
    void handle_finalizesOrderSavesAndPublishesOrderCreated() throws Exception {
        Order order = placedOrder();
        order.clearUncommittedEvents();
        when(repository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(order)));
        AtomicReference<List<DomainEvent>> published = new AtomicReference<>();
        doAnswer(inv -> {
            published.set(new ArrayList<>(inv.getArgument(0)));
            return CompletableFuture.completedFuture(null);
        }).when(messageBus).publish(any());

        handler.handle(new MarkCheckoutCompletedCommand(order.getId().value())).get();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        verify(repository).save(order);
        assertThat(published.get()).singleElement().isInstanceOf(OrderCreated.class);
    }

    @Test
    void handle_orderNotFound_throwsSynchronouslyInsideThenCompose() {
        when(repository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThatThrownBy(() -> handler.handle(new MarkCheckoutCompletedCommand(UUID.randomUUID())).get())
                .hasRootCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Order not found");
    }
}
