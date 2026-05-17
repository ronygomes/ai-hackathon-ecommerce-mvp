package me.ronygomes.ecommerce.productcatalog.application;

import me.ronygomes.ecommerce.core.domain.DomainEvent;
import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.core.infrastructure.outbox.OutboxStore;
import me.ronygomes.ecommerce.productcatalog.domain.Price;
import me.ronygomes.ecommerce.productcatalog.domain.Product;
import me.ronygomes.ecommerce.productcatalog.domain.ProductDescription;
import me.ronygomes.ecommerce.productcatalog.domain.ProductDetailsUpdated;
import me.ronygomes.ecommerce.productcatalog.domain.ProductId;
import me.ronygomes.ecommerce.productcatalog.domain.ProductName;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateProductDetailsHandlerTest {

    @SuppressWarnings("unchecked")
    private final Repository<Product, ProductId> repository = mock(Repository.class);
    private final OutboxStore outboxStore = mock(OutboxStore.class);
    private UpdateProductDetailsHandler handler;

    @BeforeEach
    void setUp() {
        handler = new UpdateProductDetailsHandler(repository, outboxStore);
        when(repository.save(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void handle_mutatesAggregateSavesAndAppendsUpdateEventToOutbox() throws Exception {
        Product existing = Product.create(new Sku("S"), new ProductName("Old"), new Price(1.0), new ProductDescription("d"));
        existing.clearUncommittedEvents();
        when(repository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(existing)));
        AtomicReference<String> appendedAggregateId = new AtomicReference<>();
        AtomicReference<List<DomainEvent>> appendedEvents = new AtomicReference<>();
        doAnswer(inv -> {
            appendedAggregateId.set(inv.getArgument(0));
            appendedEvents.set(new ArrayList<>(inv.getArgument(1)));
            return null;
        }).when(outboxStore).append(any(), any());

        handler.handle(new UpdateProductDetailsCommand(UUID.randomUUID(), "New Name", "New Desc")).get();

        assertThat(existing.getName().value()).isEqualTo("New Name");
        assertThat(existing.getDescription().value()).isEqualTo("New Desc");
        assertThat(appendedAggregateId.get()).isEqualTo(existing.getId().toString());
        assertThat(appendedEvents.get()).singleElement().isInstanceOf(ProductDetailsUpdated.class);
        verify(repository).save(existing);
    }

    @Test
    void handle_whenProductNotFound_completesExceptionally() {
        when(repository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThatThrownBy(() -> handler.handle(
                new UpdateProductDetailsCommand(UUID.randomUUID(), "x", "y")).get())
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void handle_clearsUncommittedEventsAfterAppend() throws Exception {
        Product existing = Product.create(new Sku("S"), new ProductName("Old"), new Price(1.0), new ProductDescription("d"));
        existing.clearUncommittedEvents();
        when(repository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(existing)));

        handler.handle(new UpdateProductDetailsCommand(UUID.randomUUID(), "ab", "y")).get();

        assertThat(existing.getUncommittedEvents()).isEmpty();
    }
}
