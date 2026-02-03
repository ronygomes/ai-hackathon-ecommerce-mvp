package me.ronygomes.ecommerce.ordering.infrastructure;

import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.ordering.domain.IdempotencyKey;
import me.ronygomes.ecommerce.ordering.domain.Order;
import me.ronygomes.ecommerce.ordering.domain.OrderId;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface OrderRepository extends Repository<Order, OrderId> {
    CompletableFuture<Optional<Order>> getByIdempotencyKey(IdempotencyKey idempotencyKey);
}
