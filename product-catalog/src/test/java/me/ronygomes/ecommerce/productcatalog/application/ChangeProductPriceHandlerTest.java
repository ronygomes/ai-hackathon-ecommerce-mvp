package me.ronygomes.ecommerce.productcatalog.application;

import me.ronygomes.ecommerce.core.domain.DomainEvent;
import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
import me.ronygomes.ecommerce.productcatalog.domain.Price;
import me.ronygomes.ecommerce.productcatalog.domain.Product;
import me.ronygomes.ecommerce.productcatalog.domain.ProductDescription;
import me.ronygomes.ecommerce.productcatalog.domain.ProductId;
import me.ronygomes.ecommerce.productcatalog.domain.ProductName;
import me.ronygomes.ecommerce.productcatalog.domain.ProductPriceChanged;
import me.ronygomes.ecommerce.productcatalog.domain.Sku;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChangeProductPriceHandlerTest {

    @SuppressWarnings("unchecked")
    private final Repository<Product, ProductId> repository = mock(Repository.class);
    private final MessageBus messageBus = mock(MessageBus.class);
    private ChangeProductPriceHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ChangeProductPriceHandler(repository, messageBus);
        when(repository.save(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(messageBus.publish(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void handle_changesPriceSavesAndEmitsPriceChangedEvent() throws Exception {
        Product existing = Product.create(new Sku("S"), new ProductName("Name"), new Price(10.0), new ProductDescription("d"));
        existing.clearUncommittedEvents();
        when(repository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(existing)));
        AtomicReference<List<DomainEvent>> published = new AtomicReference<>();
        doAnswer(inv -> {
            published.set(new ArrayList<>(inv.getArgument(0)));
            return CompletableFuture.completedFuture(null);
        }).when(messageBus).publish(any());

        handler.handle(new ChangeProductPriceCommand(UUID.randomUUID(), 25.0)).get();

        assertThat(existing.getPrice().value()).isEqualTo(25.0);
        assertThat(published.get()).singleElement()
                .isInstanceOfSatisfying(ProductPriceChanged.class, e -> {
                    assertThat(e.oldPrice().value()).isEqualTo(10.0);
                    assertThat(e.newPrice().value()).isEqualTo(25.0);
                });
    }

    @Test
    void handle_whenProductNotFound_completesExceptionally() {
        when(repository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThatThrownBy(() -> handler.handle(new ChangeProductPriceCommand(UUID.randomUUID(), 5.0)).get())
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void handle_negativePrice_propagatesValidationFailure() {
        Product existing = Product.create(new Sku("S"), new ProductName("Name"), new Price(10.0), new ProductDescription("d"));
        when(repository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(existing)));

        assertThatThrownBy(() -> handler.handle(new ChangeProductPriceCommand(UUID.randomUUID(), -1.0)).get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }
}
