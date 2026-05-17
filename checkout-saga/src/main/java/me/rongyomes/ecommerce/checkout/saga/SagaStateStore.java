package me.rongyomes.ecommerce.checkout.saga;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface SagaStateStore {
    Optional<SagaState> findByOrderId(UUID orderId);

    Collection<SagaState> findAll();

    void save(SagaState state);

    void remove(UUID orderId);
}
