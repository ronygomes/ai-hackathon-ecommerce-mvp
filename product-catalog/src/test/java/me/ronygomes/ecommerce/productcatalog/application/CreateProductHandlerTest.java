package me.ronygomes.ecommerce.productcatalog.application;

import me.ronygomes.ecommerce.core.domain.DomainEvent;
import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.core.infrastructure.outbox.OutboxStore;
import me.ronygomes.ecommerce.productcatalog.domain.Product;
import me.ronygomes.ecommerce.productcatalog.domain.ProductCreated;
import me.ronygomes.ecommerce.productcatalog.domain.ProductId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
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

class CreateProductHandlerTest {

    @SuppressWarnings("unchecked")
    private final Repository<Product, ProductId> repository = mock(Repository.class);
    private final OutboxStore outboxStore = mock(OutboxStore.class);
    private CreateProductHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CreateProductHandler(repository, outboxStore);
        when(repository.save(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void handle_savesProductAppendsToOutboxAndReturnsId() throws Exception {
        AtomicReference<String> appendedAggregateId = new AtomicReference<>();
        AtomicReference<List<DomainEvent>> appendedEvents = new AtomicReference<>();
        doAnswer(inv -> {
            appendedAggregateId.set(inv.getArgument(0));
            appendedEvents.set(new ArrayList<>(inv.getArgument(1)));
            return null;
        }).when(outboxStore).append(any(), any());

        ProductId result = handler.handle(new CreateProductCommand("SKU-1", "Widget", 9.99, "desc")).get();

        assertThat(result).isNotNull();

        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getSku().value()).isEqualTo("SKU-1");
        assertThat(saved.getValue().getName().value()).isEqualTo("Widget");

        assertThat(appendedAggregateId.get()).isEqualTo(result.toString());
        assertThat(appendedEvents.get()).singleElement().isInstanceOf(ProductCreated.class);
    }

    @Test
    void handle_clearsUncommittedEventsAfterAppend() throws Exception {
        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);

        handler.handle(new CreateProductCommand("SKU-1", "Widget", 1.0, "desc")).get();

        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getUncommittedEvents()).isEmpty();
    }

    @Test
    void handle_invalidDomainInputs_propagateAsFailedFuture() {
        CreateProductCommand command = new CreateProductCommand("SKU-1", "Widget", -1.0, "desc");

        assertThatThrownBy(() -> handler.handle(command).get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void handle_whenSaveFails_propagatesError() {
        when(repository.save(any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("mongo down")));

        assertThatThrownBy(() -> handler.handle(new CreateProductCommand("SKU-1", "Widget", 1.0, "d")).get())
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("mongo down");
    }
}
