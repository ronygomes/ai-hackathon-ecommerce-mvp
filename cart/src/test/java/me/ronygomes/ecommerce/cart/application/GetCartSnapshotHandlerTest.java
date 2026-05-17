package me.ronygomes.ecommerce.cart.application;

import me.ronygomes.ecommerce.checkout.saga.message.command.GetCartSnapshotCommand;
import me.ronygomes.ecommerce.checkout.saga.message.event.CartSnapshotProvided;
import me.ronygomes.ecommerce.cart.domain.CartId;
import me.ronygomes.ecommerce.cart.domain.GuestToken;
import me.ronygomes.ecommerce.cart.domain.ProductId;
import me.ronygomes.ecommerce.cart.domain.Quantity;
import me.ronygomes.ecommerce.cart.domain.ShoppingCart;
import me.ronygomes.ecommerce.cart.infrastructure.CartRepository;
import me.ronygomes.ecommerce.core.domain.DomainEvent;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
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

class GetCartSnapshotHandlerTest {

    private final CartRepository repository = mock(CartRepository.class);
    private final MessageBus messageBus = mock(MessageBus.class);
    private GetCartSnapshotHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetCartSnapshotHandler(repository, messageBus);
        when(messageBus.publish(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void handle_existingCart_publishesSnapshotWithEveryItem() throws Exception {
        UUID token = UUID.randomUUID();
        ShoppingCart cart = ShoppingCart.create(new CartId(token), new GuestToken(token.toString()));
        UUID productA = UUID.randomUUID();
        UUID productB = UUID.randomUUID();
        cart.addItem(new ProductId(productA), new Quantity(2));
        cart.addItem(new ProductId(productB), new Quantity(5));
        when(repository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(cart)));

        handler.handle(new GetCartSnapshotCommand(token.toString())).get();

        ArgumentCaptor<List<DomainEvent>> events = ArgumentCaptor.forClass(List.class);
        verify(messageBus).publish(events.capture());
        assertThat(events.getValue()).singleElement()
                .isInstanceOfSatisfying(CartSnapshotProvided.class, snapshot -> {
                    assertThat(snapshot.guestToken()).isEqualTo(token.toString());
                    assertThat(snapshot.items()).hasSize(2)
                            .extracting(CartSnapshotProvided.CartItemSnapshot::productId)
                            .containsExactlyInAnyOrder(productA, productB);
                });
    }

    @Test
    void handle_missingCart_publishesEmptySnapshot() throws Exception {
        UUID token = UUID.randomUUID();
        when(repository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        handler.handle(new GetCartSnapshotCommand(token.toString())).get();

        ArgumentCaptor<List<DomainEvent>> events = ArgumentCaptor.forClass(List.class);
        verify(messageBus).publish(events.capture());
        assertThat(events.getValue()).singleElement()
                .isInstanceOfSatisfying(CartSnapshotProvided.class,
                        snapshot -> assertThat(snapshot.items()).isEmpty());
    }
}
