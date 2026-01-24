package com.ecommerce.checkout.saga.messages.commands;

import com.ecommerce.core.application.ICommand;
import java.util.List;
import java.util.UUID;

public record CreateOrderCommand(
                UUID orderId,
                String guestToken,
                String customerName,
                String customerPhone,
                String customerEmail,
                String addressLine1,
                String addressCity,
                String addressZip,
                String addressCountry,
                String idempotencyKey,
                List<OrderItemRequest> items) implements ICommand<Void> {
        public record OrderItemRequest(
                        UUID productId,
                        String sku,
                        String name,
                        double unitPrice,
                        int qty) {
        }
}
