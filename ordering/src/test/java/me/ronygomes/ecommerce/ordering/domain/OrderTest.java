package me.ronygomes.ecommerce.ordering.domain;

import me.ronygomes.ecommerce.checkout.saga.message.event.CheckoutRequested;
import me.ronygomes.ecommerce.checkout.saga.message.event.OrderCancelled;
import me.ronygomes.ecommerce.checkout.saga.message.event.OrderCreated;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static Order placedOrder() {
        return Order.place(
                OrderId.generate(),
                new GuestToken("g1"),
                new CustomerInfo("Jane", "+1", "jane@example.com"),
                new ShippingAddress("1 Main", "Anytown", "12345", "USA"),
                List.of(new OrderLineItem(UUID.randomUUID(), "SKU-1", "Widget", 10.0, 2)),
                new IdempotencyKey(UUID.randomUUID()));
    }

    @Test
    void place_initialisesFieldsAtPendingAndEmitsCheckoutRequested() {
        OrderId orderId = OrderId.generate();
        IdempotencyKey key = new IdempotencyKey(UUID.randomUUID());

        Order order = Order.place(
                orderId,
                new GuestToken("g1"),
                new CustomerInfo("Jane", "+1", "jane@example.com"),
                new ShippingAddress("1 Main", "Anytown", "12345", "USA"),
                List.of(new OrderLineItem(UUID.randomUUID(), "SKU", "Widget", 5.0, 2)),
                key);

        assertThat(order.getId()).isEqualTo(orderId);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.getOrderNumber()).isNotNull();
        assertThat(order.getUncommittedEvents()).singleElement()
                .isInstanceOfSatisfying(CheckoutRequested.class, event -> {
                    assertThat(event.orderId()).isEqualTo(orderId.value());
                    assertThat(event.guestToken()).isEqualTo("g1");
                    assertThat(event.idempotencyKey()).isEqualTo(key.value().toString());
                });
    }

    @Test
    void finalizeCreated_transitionsToCompletedAndEmitsOrderCreated() {
        Order order = placedOrder();
        order.clearUncommittedEvents();

        order.finalizeCreated();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getUncommittedEvents()).singleElement().isInstanceOf(OrderCreated.class);
    }

    @Test
    void place_emptyItemsList_throws() {
        assertThatThrownBy(() -> Order.place(
                OrderId.generate(),
                new GuestToken("g1"),
                new CustomerInfo("Jane", "+1", "j@e"),
                new ShippingAddress("L1", "City", "x", "US"),
                List.of(),
                new IdempotencyKey(UUID.randomUUID())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no items");
    }

    @Test
    void cancel_fromPendingPayment_transitionsToCancelledAndEmitsOrderCancelled() {
        Order order = placedOrder();
        order.clearUncommittedEvents();

        order.cancel("stock validation failed");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getUncommittedEvents()).singleElement()
                .isInstanceOfSatisfying(OrderCancelled.class, e -> {
                    assertThat(e.orderId()).isEqualTo(order.getId().value().toString());
                    assertThat(e.reason()).isEqualTo("stock validation failed");
                });
    }

    @Test
    void cancel_whenAlreadyCancelled_isIdempotentNoOp() {
        Order order = placedOrder();
        order.cancel("first reason");
        order.clearUncommittedEvents();

        order.cancel("second reason");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getUncommittedEvents()).isEmpty();
    }

    @Test
    void cancel_afterConfirmed_throwsIllegalStateException() {
        Order order = placedOrder();
        order.finalizeCreated();

        assertThatThrownBy(() -> order.cancel("late cancel"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel a confirmed order");
    }
}
