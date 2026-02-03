package me.ronygomes.ecommerce.ordering.application;

import me.ronygomes.ecommerce.core.application.Command;
import me.ronygomes.ecommerce.ordering.domain.CustomerInfo;
import me.ronygomes.ecommerce.ordering.domain.ShippingAddress;
import java.util.List;
import java.util.UUID;

public record PlaceOrderCommand(
        String guestToken,
        String cartId,
        CustomerInfo customerInfo,
        ShippingAddress address,
        String idempotencyKey,
        List<OrderItemRequest> items) implements Command<UUID> {
    public record OrderItemRequest(
            UUID productId,
            String sku,
            String name,
            double unitPrice,
            int qty) {
    }
}
