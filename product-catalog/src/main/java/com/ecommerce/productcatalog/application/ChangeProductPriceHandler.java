package com.ecommerce.productcatalog.application;

import com.ecommerce.core.application.ICommandHandler;
import com.ecommerce.core.infrastructure.IRepository;
import com.ecommerce.core.messaging.IMessageBus;
import com.ecommerce.productcatalog.domain.*;
import com.google.inject.Inject;
import java.util.concurrent.CompletableFuture;

public class ChangeProductPriceHandler implements ICommandHandler<ChangeProductPriceCommand, Void> {
    private final IRepository<Product, ProductId> repository;
    private final IMessageBus messageBus;

    @Inject
    public ChangeProductPriceHandler(IRepository<Product, ProductId> repository, IMessageBus messageBus) {
        this.repository = repository;
        this.messageBus = messageBus;
    }

    @Override
    public CompletableFuture<Void> handle(ChangeProductPriceCommand command) {
        return repository.getById(new ProductId(command.productId()))
                .thenCompose(opt -> opt.map(product -> {
                    product.changePrice(new Price(command.newPrice()));
                    return repository.save(product)
                            .thenCompose(v -> messageBus.publish(product.getUncommittedEvents()))
                            .thenRun(product::clearUncommittedEvents);
                }).orElse(CompletableFuture.failedFuture(new RuntimeException("Product not found"))));
    }
}
