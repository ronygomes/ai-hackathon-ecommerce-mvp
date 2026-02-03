package me.ronygomes.ecommerce.productcatalog.application;

import me.ronygomes.ecommerce.core.application.CommandHandler;
import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
import me.rongyomes.ecommerce.checkout.saga.message.command.GetProductSnapshotsCommand;
import me.rongyomes.ecommerce.checkout.saga.message.event.ProductSnapshotsProvided;
import com.google.inject.Inject;
import me.ronygomes.ecommerce.productcatalog.domain.Product;
import me.ronygomes.ecommerce.productcatalog.domain.ProductId;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;
import java.util.Optional;

public class GetProductSnapshotsHandler implements CommandHandler<GetProductSnapshotsCommand, Void> {
    private final Repository<Product, ProductId> repository;
    private final MessageBus messageBus;

    @Inject
    public GetProductSnapshotsHandler(Repository<Product, ProductId> repository, MessageBus messageBus) {
        this.repository = repository;
        this.messageBus = messageBus;
    }

    @Override
    public CompletableFuture<Void> handle(GetProductSnapshotsCommand command) {
        return CompletableFuture.runAsync(() -> {
            try {
                List<ProductSnapshotsProvided.ProductSnapshot> snapshots = new ArrayList<>();
                for (UUID id : command.productIds()) {
                    Optional<Product> p = repository.getById(ProductId.fromString(id.toString())).get();
                    if (p.isPresent()) {
                        Product prod = p.get();
                        snapshots.add(
                                new ProductSnapshotsProvided.ProductSnapshot(
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
