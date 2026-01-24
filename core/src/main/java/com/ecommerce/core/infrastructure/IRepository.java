package com.ecommerce.core.infrastructure;

import com.ecommerce.core.domain.IAggregateRoot;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;

public interface IRepository<TAggregate extends IAggregateRoot<TId>, TId> {
    CompletableFuture<Optional<TAggregate>> getById(TId id);

    CompletableFuture<Void> save(TAggregate aggregate);
}
