package me.ronygomes.ecommerce.productcatalog.application;

import me.ronygomes.ecommerce.checkout.saga.message.command.GetProductSnapshotsCommand;
import me.ronygomes.ecommerce.checkout.saga.message.event.ProductSnapshotsProvided;
import me.ronygomes.ecommerce.core.domain.DomainEvent;
import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
import me.ronygomes.ecommerce.productcatalog.domain.Price;
import me.ronygomes.ecommerce.productcatalog.domain.Product;
import me.ronygomes.ecommerce.productcatalog.domain.ProductDescription;
import me.ronygomes.ecommerce.productcatalog.domain.ProductId;
import me.ronygomes.ecommerce.productcatalog.domain.ProductName;
import me.ronygomes.ecommerce.productcatalog.domain.Sku;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetProductSnapshotsHandlerTest {

    @SuppressWarnings("unchecked")
    private final Repository<Product, ProductId> repository = mock(Repository.class);
    private final MessageBus messageBus = mock(MessageBus.class);
    private GetProductSnapshotsHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetProductSnapshotsHandler(repository, messageBus);
        when(messageBus.publish(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    private static Product activeProduct(String sku, String name, double price) {
        Product p = Product.create(ProductId.generate(), new Sku(sku), new ProductName(name), new Price(price), new ProductDescription("d"));
        p.activate();
        return p;
    }

    @Test
    void handle_publishesSnapshotsForEveryFoundProduct() throws Exception {
        Product a = activeProduct("SKU-A", "Apple", 1.0);
        Product b = activeProduct("SKU-B", "Banana", 2.0);
        when(repository.getById(any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(a)))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(b)));

        UUID correlationId = UUID.randomUUID();
        handler.handle(new GetProductSnapshotsCommand(
                List.of(UUID.randomUUID(), UUID.randomUUID()), correlationId)).get();

        ArgumentCaptor<List<DomainEvent>> events = ArgumentCaptor.forClass(List.class);
        verify(messageBus).publish(events.capture());
        assertThat(events.getValue()).singleElement()
                .isInstanceOfSatisfying(ProductSnapshotsProvided.class, evt -> {
                    assertThat(evt.correlationId()).isEqualTo(correlationId);
                    assertThat(evt.snapshots())
                            .extracting(ProductSnapshotsProvided.ProductSnapshot::sku)
                            .containsExactly("SKU-A", "SKU-B");
                });
    }

    @Test
    void handle_missingProductsAreOmittedFromSnapshotList() throws Exception {
        Product a = activeProduct("SKU-A", "Apple", 1.0);
        when(repository.getById(any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(a)))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        handler.handle(new GetProductSnapshotsCommand(
                List.of(UUID.randomUUID(), UUID.randomUUID()), UUID.randomUUID())).get();

        ArgumentCaptor<List<DomainEvent>> events = ArgumentCaptor.forClass(List.class);
        verify(messageBus).publish(events.capture());
        assertThat(events.getValue()).singleElement()
                .isInstanceOfSatisfying(ProductSnapshotsProvided.class, evt ->
                        assertThat(evt.snapshots()).hasSize(1));
    }

    @Test
    void handle_emptyIdList_publishesEmptySnapshotEventEchoingCorrelationId() throws Exception {
        UUID correlationId = UUID.randomUUID();
        handler.handle(new GetProductSnapshotsCommand(List.of(), correlationId)).get();

        ArgumentCaptor<List<DomainEvent>> events = ArgumentCaptor.forClass(List.class);
        verify(messageBus).publish(events.capture());
        assertThat(events.getValue()).singleElement()
                .isInstanceOfSatisfying(ProductSnapshotsProvided.class, evt -> {
                    assertThat(evt.snapshots()).isEmpty();
                    assertThat(evt.correlationId()).isEqualTo(correlationId);
                });
    }
}
