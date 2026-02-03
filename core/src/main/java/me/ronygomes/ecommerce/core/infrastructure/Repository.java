package me.ronygomes.ecommerce.core.infrastructure;

import me.ronygomes.ecommerce.core.domain.AggregateRoot;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface Repository<TAggregate extends AggregateRoot<TId>, TId> {
    CompletableFuture<Optional<TAggregate>> getById(TId id);

    CompletableFuture<Void> save(TAggregate aggregate);
}
