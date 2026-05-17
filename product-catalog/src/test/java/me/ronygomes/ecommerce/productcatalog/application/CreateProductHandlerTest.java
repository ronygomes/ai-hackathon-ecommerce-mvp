package me.ronygomes.ecommerce.productcatalog.application;

import me.ronygomes.ecommerce.core.domain.DomainEvent;
import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
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
    private final MessageBus messageBus = mock(MessageBus.class);
    private CreateProductHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CreateProductHandler(repository, messageBus);
        when(repository.save(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(messageBus.publish(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void handle_savesProductPublishesEventsAndReturnsId() throws Exception {
        AtomicReference<List<DomainEvent>> published = new AtomicReference<>();
        doAnswer(inv -> {
            published.set(new ArrayList<>(inv.getArgument(0)));
            return CompletableFuture.completedFuture(null);
        }).when(messageBus).publish(any());

        ProductId result = handler.handle(new CreateProductCommand("SKU-1", "Widget", 9.99, "desc")).get();

        assertThat(result).isNotNull();

        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getSku().value()).isEqualTo("SKU-1");
        assertThat(saved.getValue().getName().value()).isEqualTo("Widget");
        assertThat(saved.getValue().getPrice().value()).isEqualTo(9.99);

        assertThat(published.get()).singleElement().isInstanceOf(ProductCreated.class);
    }

    @Test
    void handle_clearsUncommittedEventsAfterPublish() throws Exception {
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
