package com.ecommerce.productcatalog.application;

import com.ecommerce.core.application.ICommandHandler;
import com.ecommerce.core.infrastructure.IRepository;
import com.ecommerce.core.messaging.IMessageBus;
import com.ecommerce.productcatalog.domain.*;
import com.google.inject.Inject;
import java.util.concurrent.CompletableFuture;

public class CreateProductHandler implements ICommandHandler<CreateProductCommand, ProductId> {
    private final IRepository<Product, ProductId> repository;
    private final IMessageBus messageBus;

    @Inject
    public CreateProductHandler(IRepository<Product, ProductId> repository, IMessageBus messageBus) {
        this.repository = repository;
        this.messageBus = messageBus;
    }

    @Override
    public CompletableFuture<ProductId> handle(CreateProductCommand command) {
        return CompletableFuture.supplyAsync(() -> {
            Product product = Product.create(
                    new Sku(command.sku()),
                    new ProductName(command.name()),
                    new Price(command.price()),
                    new ProductDescription(command.description()));
            return product;
        }).thenCompose(product -> repository.save(product)
                .thenCompose(v -> messageBus.publish(product.getUncommittedEvents()))
                .thenApply(v -> {
                    product.clearUncommittedEvents();
                    return product.getId();
                }));
    }
}
