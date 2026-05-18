package me.ronygomes.ecommerce.cart.application;

import me.ronygomes.ecommerce.cart.domain.CartCreated;
import me.ronygomes.ecommerce.cart.domain.CartId;
import me.ronygomes.ecommerce.cart.domain.CartItemAdded;
import me.ronygomes.ecommerce.cart.domain.GuestToken;
import me.ronygomes.ecommerce.cart.domain.ShoppingCart;
import me.ronygomes.ecommerce.cart.infrastructure.CartRepository;
import me.ronygomes.ecommerce.core.domain.DomainEvent;
import me.ronygomes.ecommerce.core.infrastructure.outbox.OutboxStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AddCartItemHandlerTest {

    private final CartRepository repository = mock(CartRepository.class);
    private final OutboxStore outboxStore = mock(OutboxStore.class);
    private AddCartItemHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AddCartItemHandler(repository, outboxStore);
        when(repository.save(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void handle_existingCart_appendsItemAndPushesCartItemAddedToOutbox() throws Exception {
        UUID token = UUID.randomUUID();
        ShoppingCart existing = ShoppingCart.create(new CartId(token), new GuestToken(token.toString()));
        existing.clearUncommittedEvents(); // discard CartCreated from setup so this test only sees add
        when(repository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(existing)));
        UUID productId = UUID.randomUUID();
        AtomicReference<List<DomainEvent>> appended = snapshotAppendedEvents();

        handler.handle(new AddCartItemCommand(token.toString(), productId, 3)).get();

        assertThat(existing.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getProductId().value()).isEqualTo(productId);
            assertThat(item.getQuantity().value()).isEqualTo(3);
        });
        verify(repository).save(existing);
        assertThat(appended.get()).singleElement()
                .isInstanceOfSatisfying(CartItemAdded.class, e -> {
                    assertThat(e.cartId()).isEqualTo(token);
                    assertThat(e.productId()).isEqualTo(productId);
                    assertThat(e.qty()).isEqualTo(3);
                });
    }

    @Test
    void handle_missingCart_createsNewOneAndPushesCartCreatedThenCartItemAdded() throws Exception {
        UUID token = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(repository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        AtomicReference<List<DomainEvent>> appended = snapshotAppendedEvents();

        handler.handle(new AddCartItemCommand(token.toString(), productId, 2)).get();

        ArgumentCaptor<ShoppingCart> saved = ArgumentCaptor.forClass(ShoppingCart.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getId().value()).isEqualTo(token);
        assertThat(saved.getValue().getItems()).hasSize(1);
        assertThat(appended.get()).hasSize(2);
        assertThat(appended.get().get(0)).isInstanceOfSatisfying(CartCreated.class, e -> {
            assertThat(e.cartId()).isEqualTo(token);
            assertThat(e.guestToken()).isEqualTo(token.toString());
        });
        assertThat(appended.get().get(1)).isInstanceOfSatisfying(CartItemAdded.class, e -> {
            assertThat(e.cartId()).isEqualTo(token);
            assertThat(e.productId()).isEqualTo(productId);
            assertThat(e.qty()).isEqualTo(2);
        });
    }

    /**
     * Captures the event list as-it-was at append() invocation. Required because the
     * handler calls cart.clearUncommittedEvents() right after outboxStore.append(...),
     * and getUncommittedEvents() returns an unmodifiable view — by verify time the list
     * looks empty even though it had events when appended.
     */
    private AtomicReference<List<DomainEvent>> snapshotAppendedEvents() {
        AtomicReference<List<DomainEvent>> ref = new AtomicReference<>(List.of());
        doAnswer(inv -> {
            ref.set(new ArrayList<>(inv.getArgument(1)));
            return null;
        }).when(outboxStore).append(any(), any());
        return ref;
    }
}
