package me.ronygomes.ecommerce.ordering.application;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import me.ronygomes.ecommerce.core.application.Command;
import me.ronygomes.ecommerce.ordering.domain.CustomerInfo;
import me.ronygomes.ecommerce.ordering.domain.ShippingAddress;

import java.util.List;
import java.util.UUID;

public record PlaceOrderCommand(
        @NotNull UUID orderId,
        @NotBlank(message = "guestToken cannot be empty") String guestToken,
        @NotBlank(message = "cartId cannot be empty") String cartId,
        @NotNull CustomerInfo customerInfo,
        @NotNull(message = "address cannot be null") ShippingAddress address,
        @NotBlank(message = "idempotencyKey cannot be empty") String idempotencyKey,
        @NotEmpty(message = "items cannot be empty") List<OrderItemRequest> items) implements Command<UUID> {
    public record OrderItemRequest(
            @NotNull UUID productId,
            @NotBlank String sku,
            @NotBlank String name,
            double unitPrice,
            @Min(value = 1, message = "qty must be >= 1") int qty) {
    }
}
