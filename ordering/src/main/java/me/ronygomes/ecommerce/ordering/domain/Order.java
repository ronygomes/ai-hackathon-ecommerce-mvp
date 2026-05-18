package me.ronygomes.ecommerce.ordering.domain;

import me.ronygomes.ecommerce.checkout.saga.message.event.CheckoutRequested;
import me.ronygomes.ecommerce.checkout.saga.message.event.OrderCreated;
import me.ronygomes.ecommerce.core.domain.BaseAggregate;

import java.util.List;

public class Order extends BaseAggregate<OrderId> {
    private OrderNumber orderNumber;
    private GuestToken guestToken;
    private CustomerInfo customerInfo;
    private ShippingAddress shippingAddress;
    private List<OrderLineItem> items;
    private OrderTotals totals;
    private IdempotencyKey idempotencyKey;
    private OrderStatus status;

    // Required for Jackson — populates fields via reflection (see BaseMongoRepository.aggregateMapper).
    private Order() {
    }

    private Order(OrderId id, OrderNumber orderNumber, GuestToken guestToken, CustomerInfo customerInfo,
            ShippingAddress shippingAddress, List<OrderLineItem> items, OrderTotals totals,
            IdempotencyKey idempotencyKey) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.guestToken = guestToken;
        this.customerInfo = customerInfo;
        this.shippingAddress = shippingAddress;
        this.items = items;
        this.totals = totals;
        this.idempotencyKey = idempotencyKey;
        this.status = OrderStatus.PENDING_PAYMENT;
    }

    public static Order place(OrderId id, GuestToken guestToken, CustomerInfo customerInfo, ShippingAddress address,
            List<OrderLineItem> items, IdempotencyKey idempotencyKey) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Cannot place an order with no items");
        }
        OrderNumber orderNumber = OrderNumber.generate();
        OrderTotals totals = OrderTotals.calculate(items);
        Order order = new Order(id, orderNumber, guestToken, customerInfo, address, items, totals,
                idempotencyKey);

        // Notify saga to start
        order.addEvent(new CheckoutRequested(
                id.value(),
                guestToken.value(),
                guestToken.value(), // cartId is guestToken in MVP
                customerInfo.name(),
                customerInfo.phone(),
                customerInfo.email(),
                address.line1(),
                address.city(),
                address.postalCode(),
                address.country(),
                idempotencyKey.value().toString()));

        return order;
    }

    public void finalizeCreated() {
        this.status = OrderStatus.CONFIRMED;
        // Notify saga step or other systems that the order is now finalized
        addEvent(new OrderCreated(id.value().toString(), guestToken.value(), customerInfo.email()));
    }

    public OrderNumber getOrderNumber() {
        return orderNumber;
    }

    public OrderStatus getStatus() {
        return status;
    }
}
