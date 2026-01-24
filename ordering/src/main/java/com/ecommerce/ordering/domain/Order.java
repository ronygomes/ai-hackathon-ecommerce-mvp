package com.ecommerce.ordering.domain;

import com.ecommerce.core.domain.BaseAggregate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Order extends BaseAggregate<OrderId> {
    private OrderNumber orderNumber;
    private GuestToken guestToken;
    private CustomerInfo customerInfo;
    private ShippingAddress shippingAddress;
    private List<OrderLineItem> items = new ArrayList<>();
    private OrderTotals totals;
    private IdempotencyKey idempotencyKey;
    private String paymentMethod = "COD";
    private String paymentStatus = "Pending";
    private boolean stockCommitted = false;
    private boolean completed = false;

    public Order() {
        // Required for Jackson
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
    }

    public static Order place(OrderId id, GuestToken guestToken, CustomerInfo customerInfo,
            ShippingAddress shippingAddress, List<OrderLineItem> items, IdempotencyKey idempotencyKey) {

        if (items.isEmpty()) {
            throw new IllegalArgumentException("Cannot place an order with no items");
        }

        double subtotal = items.stream().mapToDouble(OrderLineItem::getLineTotal).sum();
        OrderTotals totals = OrderTotals.calculate(subtotal, 0.0);
        OrderNumber orderNumber = OrderNumber.generate();

        Order order = new Order(id, orderNumber, guestToken, customerInfo, shippingAddress, items, totals,
                idempotencyKey);

        order.addEvent(new OrderCreated(id.value(), orderNumber.value(), guestToken.value(), customerInfo,
                shippingAddress, totals, items));

        List<OrderStockCommitRequested.StockItem> stockItems = items.stream()
                .map(i -> new OrderStockCommitRequested.StockItem(i.getProductId(), i.getQuantity()))
                .collect(Collectors.toList());

        order.addEvent(new OrderStockCommitRequested(id.value(), stockItems));

        return order;
    }

    public void markStockCommitted() {
        this.stockCommitted = true;
        this.addEvent(new OrderStockCommitted(this.id.value()));
    }

    public void finalizeCreated() {
        this.completed = true;
        // This is where we might publish the final OrderSubmitted integration event
        this.addEvent(new OrderSubmitted(this.id.value(), this.orderNumber.value(), this.guestToken.value(), this.items,
                this.totals));
    }

    public OrderNumber getOrderNumber() {
        return orderNumber;
    }

    public GuestToken getGuestToken() {
        return guestToken;
    }

    public CustomerInfo getCustomerInfo() {
        return customerInfo;
    }

    public ShippingAddress getShippingAddress() {
        return shippingAddress;
    }

    public List<OrderLineItem> getItems() {
        return items;
    }

    public OrderTotals getTotals() {
        return totals;
    }

    public IdempotencyKey getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public boolean isStockCommitted() {
        return stockCommitted;
    }

    public boolean isCompleted() {
        return completed;
    }
}
