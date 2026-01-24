package com.ecommerce.ordering.infrastructure;

import com.ecommerce.core.infrastructure.IRepository;
import com.ecommerce.ordering.domain.Order;
import com.ecommerce.ordering.domain.OrderId;
import com.ecommerce.ordering.domain.IdempotencyKey;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface IOrderRepository extends IRepository<Order, OrderId> {
    CompletableFuture<Optional<Order>> getByIdempotencyKey(IdempotencyKey idempotencyKey);
}
