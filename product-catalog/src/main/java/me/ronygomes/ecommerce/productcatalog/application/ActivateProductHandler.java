package me.ronygomes.ecommerce.productcatalog.application;

import com.google.inject.Inject;
import me.ronygomes.ecommerce.core.application.CommandHandler;
import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.core.infrastructure.outbox.OutboxStore;
import me.ronygomes.ecommerce.productcatalog.domain.Product;
import me.ronygomes.ecommerce.productcatalog.domain.ProductId;

import java.util.concurrent.CompletableFuture;

public class ActivateProductHandler implements CommandHandler<ActivateProductCommand, Void> {
    private final Repository<Product, ProductId> repository;
    private final OutboxStore outboxStore;

    @Inject
    public ActivateProductHandler(Repository<Product, ProductId> repository, OutboxStore outboxStore) {
        this.repository = repository;
        this.outboxStore = outboxStore;
    }

    @Override
    public CompletableFuture<Void> handle(ActivateProductCommand command) {
        return repository.getById(new ProductId(command.productId()))
                .thenCompose(opt -> opt.map(product -> {
                    product.activate();
                    return repository.save(product)
                            .thenAccept(v -> {
                                outboxStore.append(product.getId().toString(), product.getUncommittedEvents());
                                product.clearUncommittedEvents();
                            });
                }).orElse(CompletableFuture.failedFuture(new RuntimeException("Product not found"))));
    }
}
