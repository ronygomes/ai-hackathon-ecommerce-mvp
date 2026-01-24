package com.ecommerce.productcatalog.application;

import com.ecommerce.core.application.ICommandHandler;
import com.ecommerce.core.infrastructure.IRepository;
import com.ecommerce.core.messaging.IMessageBus;
import com.ecommerce.productcatalog.domain.*;
import com.ecommerce.checkout.saga.messages.commands.GetProductSnapshotsCommand;
import com.ecommerce.checkout.saga.messages.events.ProductSnapshotsProvided;
import com.google.inject.Inject;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.UUID;
import java.util.Optional;

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
        return CompletableFuture.runAsync(() -> {
            try {
                List<com.ecommerce.checkout.saga.messages.events.ProductSnapshotsProvided.ProductSnapshot> snapshots = new ArrayList<>();
                for (UUID id : command.productIds()) {
                    Optional<Product> p = repository.getById(ProductId.fromString(id.toString())).get();
                    if (p.isPresent()) {
                        Product prod = p.get();
                        snapshots.add(
                                new com.ecommerce.checkout.saga.messages.events.ProductSnapshotsProvided.ProductSnapshot(
                                        prod.getId().value(),
                                        prod.getSku().value(),
                                        prod.getName().value(),
                                        prod.getPrice().value(),
                                        prod.isActive()));
                    }
                }
                messageBus.publish(List.of(new ProductSnapshotsProvided(snapshots)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
