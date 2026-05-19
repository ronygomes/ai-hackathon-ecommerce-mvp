package me.ronygomes.ecommerce.ordering.application;

import me.ronygomes.ecommerce.checkout.saga.message.command.CancelOrderCommand;
import me.ronygomes.ecommerce.checkout.saga.message.event.OrderCancelled;
import me.ronygomes.ecommerce.core.domain.DomainEvent;
import me.ronygomes.ecommerce.core.infrastructure.outbox.OutboxStore;
import me.ronygomes.ecommerce.ordering.domain.*;
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
import static org.mockito.Mockito.*;

class CancelOrderHandlerTest {

    private final OrderRepository repository = mock(OrderRepository.class);
    private final OutboxStore outboxStore = mock(OutboxStore.class);
    private CancelOrderHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CancelOrderHandler(repository, outboxStore);
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
    void handle_cancelsOrderSavesAndAppendsOrderCancelledToOutbox() throws Exception {
        Order order = placedOrder();
        order.clearUncommittedEvents();
        when(repository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(order)));
        AtomicReference<String> appendedAggregateId = new AtomicReference<>();
        AtomicReference<List<DomainEvent>> appendedEvents = new AtomicReference<>();
        doAnswer(inv -> {
            appendedAggregateId.set(inv.getArgument(0));
            appendedEvents.set(new ArrayList<>(inv.getArgument(1)));
            return null;
        }).when(outboxStore).append(any(), any());

        handler.handle(new CancelOrderCommand(order.getId().value(), "stock validation failed")).get();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(repository).save(order);
        assertThat(appendedAggregateId.get()).isEqualTo(order.getId().toString());
        assertThat(appendedEvents.get()).singleElement()
                .isInstanceOfSatisfying(OrderCancelled.class,
                        e -> assertThat(e.reason()).isEqualTo("stock validation failed"));
    }

    @Test
    void handle_orderNotFound_returnsFailedFuture() {
        when(repository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThatThrownBy(() -> handler.handle(new CancelOrderCommand(UUID.randomUUID(), "x")).get())
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Order not found");
    }
}
