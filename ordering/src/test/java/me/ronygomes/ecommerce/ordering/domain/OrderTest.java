package me.ronygomes.ecommerce.ordering.domain;

import me.rongyomes.ecommerce.checkout.saga.message.event.CheckoutRequested;
import me.rongyomes.ecommerce.checkout.saga.message.event.OrderCreated;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
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

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.getUncommittedEvents()).singleElement().isInstanceOf(OrderCreated.class);
    }

    @Test
    void place_acceptsEmptyItemsList_currentBehavior() {
        // Spec (prompt-7) says order should only be created if cart is non-empty.
        // Current impl performs no such check. Pinned so a future fix is intentional.
        Order order = Order.place(
                OrderId.generate(),
                new GuestToken("g1"),
                new CustomerInfo("Jane", "+1", "j@e"),
                new ShippingAddress("L1", "City", "x", "US"),
                List.of(),
                new IdempotencyKey(UUID.randomUUID()));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }
}
