package me.ronygomes.ecommerce.checkout.saga;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySagaStateStore implements SagaStateStore {
    private final Map<UUID, SagaState> store = new ConcurrentHashMap<>();

    @Override
    public Optional<SagaState> findByOrderId(UUID orderId) {
        return Optional.ofNullable(store.get(orderId));
    }

    @Override
    public Optional<SagaState> findByCorrelationId(UUID correlationId) {
        return store.values().stream()
                .filter(s -> correlationId.equals(s.correlationId))
                .findFirst();
    }

    @Override
    public Collection<SagaState> findAll() {
        return store.values();
    }

    @Override
    public void save(SagaState state) {
        store.put(state.orderId, state);
    }

    @Override
    public void remove(UUID orderId) {
        store.remove(orderId);
    }
}
