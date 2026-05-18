package me.ronygomes.ecommerce.productcatalog.application;

import com.google.inject.Inject;
import me.ronygomes.ecommerce.core.application.CommandHandler;
import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.core.infrastructure.outbox.OutboxStore;
import me.ronygomes.ecommerce.productcatalog.domain.*;

import java.util.concurrent.CompletableFuture;

public class CreateProductHandler implements CommandHandler<CreateProductCommand, ProductId> {
    private final Repository<Product, ProductId> repository;
    private final OutboxStore outboxStore;

    @Inject
    public CreateProductHandler(Repository<Product, ProductId> repository, OutboxStore outboxStore) {
        this.repository = repository;
        this.outboxStore = outboxStore;
    }

    @Override
    public CompletableFuture<ProductId> handle(CreateProductCommand command) {
        return CompletableFuture.supplyAsync(() -> Product.create(
                        new ProductId(command.productId()),
                        new Sku(command.sku()),
                        new ProductName(command.name()),
                        new Price(command.price()),
                        new ProductDescription(command.description())))
                .thenCompose(product -> repository.save(product)
                        .thenApply(v -> {
                            outboxStore.append(product.getId().toString(), product.getUncommittedEvents());
                            product.clearUncommittedEvents();
                            return product.getId();
                        }));
    }
}
