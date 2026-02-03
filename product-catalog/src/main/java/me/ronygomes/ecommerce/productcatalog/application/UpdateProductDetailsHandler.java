package me.ronygomes.ecommerce.productcatalog.application;

import me.ronygomes.ecommerce.core.application.CommandHandler;
import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
import com.google.inject.Inject;
import me.ronygomes.ecommerce.productcatalog.domain.Product;
import me.ronygomes.ecommerce.productcatalog.domain.ProductDescription;
import me.ronygomes.ecommerce.productcatalog.domain.ProductId;
import me.ronygomes.ecommerce.productcatalog.domain.ProductName;

import java.util.concurrent.CompletableFuture;

public class UpdateProductDetailsHandler implements CommandHandler<UpdateProductDetailsCommand, Void> {
    private final Repository<Product, ProductId> repository;
    private final MessageBus messageBus;

    @Inject
    public UpdateProductDetailsHandler(Repository<Product, ProductId> repository, MessageBus messageBus) {
        this.repository = repository;
        this.messageBus = messageBus;
    }

    @Override
    public CompletableFuture<Void> handle(UpdateProductDetailsCommand command) {
        return repository.getById(new ProductId(command.productId()))
                .thenCompose(opt -> opt.map(product -> {
                    product.updateDetails(new ProductName(command.name()),
                            new ProductDescription(command.description()));
                    return repository.save(product)
                            .thenCompose(v -> messageBus.publish(product.getUncommittedEvents()))
                            .thenRun(product::clearUncommittedEvents);
                }).orElse(CompletableFuture.failedFuture(new RuntimeException("Product not found"))));
    }
}
