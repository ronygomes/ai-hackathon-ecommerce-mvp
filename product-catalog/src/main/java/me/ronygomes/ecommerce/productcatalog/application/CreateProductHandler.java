package me.ronygomes.ecommerce.productcatalog.application;

import com.google.inject.Inject;
import me.ronygomes.ecommerce.core.application.CommandHandler;
import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
import me.ronygomes.ecommerce.productcatalog.domain.*;

import java.util.concurrent.CompletableFuture;

public class CreateProductHandler implements CommandHandler<CreateProductCommand, ProductId> {
    private final Repository<Product, ProductId> repository;
    private final MessageBus messageBus;

    @Inject
    public CreateProductHandler(Repository<Product, ProductId> repository, MessageBus messageBus) {
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
