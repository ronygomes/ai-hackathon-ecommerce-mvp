package me.ronygomes.ecommerce.productcatalog.application;

import com.google.inject.Inject;
import me.ronygomes.ecommerce.core.application.CommandHandler;
import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.core.infrastructure.outbox.OutboxStore;
import me.ronygomes.ecommerce.productcatalog.domain.Price;
import me.ronygomes.ecommerce.productcatalog.domain.Product;
import me.ronygomes.ecommerce.productcatalog.domain.ProductId;

import java.util.concurrent.CompletableFuture;

public class ChangeProductPriceHandler implements CommandHandler<ChangeProductPriceCommand, Void> {
    private final Repository<Product, ProductId> repository;
    private final OutboxStore outboxStore;

    @Inject
    public ChangeProductPriceHandler(Repository<Product, ProductId> repository, OutboxStore outboxStore) {
        this.repository = repository;
        this.outboxStore = outboxStore;
    }

    @Override
    public CompletableFuture<Void> handle(ChangeProductPriceCommand command) {
        return repository.getById(new ProductId(command.productId()))
                .thenCompose(opt -> opt.map(product -> {
                    product.changePrice(new Price(command.newPrice()));
                    return repository.save(product)
                            .thenAccept(v -> {
                                outboxStore.append(product.getId().toString(), product.getUncommittedEvents());
                                product.clearUncommittedEvents();
                            });
                }).orElse(CompletableFuture.failedFuture(new RuntimeException("Product not found"))));
    }
}
