package com.ecommerce.ordering.application;

import com.ecommerce.core.application.ICommand;
import com.ecommerce.ordering.domain.CustomerInfo;
import com.ecommerce.ordering.domain.ShippingAddress;
import java.util.List;
import java.util.UUID;

public record PlaceOrderCommand(
        String guestToken,
        String cartId,
        CustomerInfo customerInfo,
        ShippingAddress address,
        String idempotencyKey,
        List<OrderItemRequest> items) implements ICommand<UUID> {
    public record OrderItemRequest(
            UUID productId,
            String sku,
            String name,
            double unitPrice,
            int qty) {
    }
}
