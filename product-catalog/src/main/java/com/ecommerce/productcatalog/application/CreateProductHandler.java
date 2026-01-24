package com.ecommerce.productcatalog.application;

import com.ecommerce.core.application.ICommandHandler;
import com.ecommerce.core.infrastructure.IRepository;
import com.ecommerce.core.messaging.IMessageBus;
import com.ecommerce.productcatalog.domain.Product;
import com.google.inject.Inject;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CreateProductHandler implements ICommandHandler<CreateProductCommand, UUID> {
    private final IRepository<Product, UUID> repository;
    private final IMessageBus messageBus;

    @Inject
    public CreateProductHandler(IRepository<Product, UUID> repository, IMessageBus messageBus) {
        this.repository = repository;
        this.messageBus = messageBus;
    }

    @Override
    public CompletableFuture<UUID> handle(CreateProductCommand command) {
        return CompletableFuture.supplyAsync(() -> {
            Product product = Product.create(command.name(), command.sku(), command.price(), command.description());
            return product;
        }).thenCompose(product -> repository.save(product)
                .thenCompose(v -> messageBus.publish(product.getUncommittedEvents()))
                .thenApply(v -> {
                    product.clearUncommittedEvents();
                    return product.getId();
                }));
    }
}
