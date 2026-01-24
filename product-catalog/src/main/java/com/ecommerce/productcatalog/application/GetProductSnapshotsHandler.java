package com.ecommerce.productcatalog.application;

import com.ecommerce.core.application.ICommandHandler;
import com.ecommerce.core.infrastructure.IRepository;
import com.ecommerce.core.messaging.IMessageBus;
import com.ecommerce.productcatalog.domain.*;
import com.google.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class GetProductSnapshotsHandler implements ICommandHandler<GetProductSnapshotsCommand, Void> {
    private final IRepository<Product, ProductId> repository;
    private final IMessageBus messageBus;

    @Inject
    public GetProductSnapshotsHandler(IRepository<Product, ProductId> repository, IMessageBus messageBus) {
        this.repository = repository;
        this.messageBus = messageBus;
    }

    @Override
    public CompletableFuture<Void> handle(GetProductSnapshotsCommand command) {
        List<CompletableFuture<ProductSnapshotsProvided.ProductSnapshot>> lookups = command.productIds().stream()
                .map(id -> repository.getById(new ProductId(id))
                        .thenApply(opt -> opt.map(p -> new ProductSnapshotsProvided.ProductSnapshot(
                                p.getId().value(), p.getSku().value(), p.getName().value(), p.getPrice().value(),
                                p.isActive())).orElseThrow(() -> new RuntimeException("Product not found: " + id))))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(lookups.toArray(new CompletableFuture[0]))
                .thenCompose(v -> {
                    List<ProductSnapshotsProvided.ProductSnapshot> snapshots = lookups.stream()
                            .map(CompletableFuture::join).collect(Collectors.toList());
                    return messageBus.publish(List.of(new ProductSnapshotsProvided(snapshots)));
                });
    }
}
