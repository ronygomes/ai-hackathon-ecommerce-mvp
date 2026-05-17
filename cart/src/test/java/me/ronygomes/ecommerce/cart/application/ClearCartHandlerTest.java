package me.ronygomes.ecommerce.cart.application;

import me.ronygomes.ecommerce.checkout.saga.message.command.ClearCartCommand;
import me.ronygomes.ecommerce.checkout.saga.message.event.CartCleared;
import me.ronygomes.ecommerce.cart.domain.CartId;
import me.ronygomes.ecommerce.cart.domain.GuestToken;
import me.ronygomes.ecommerce.cart.domain.ProductId;
import me.ronygomes.ecommerce.cart.domain.Quantity;
import me.ronygomes.ecommerce.cart.domain.ShoppingCart;
import me.ronygomes.ecommerce.cart.infrastructure.CartRepository;
import me.ronygomes.ecommerce.core.domain.DomainEvent;
import me.ronygomes.ecommerce.core.infrastructure.outbox.OutboxStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClearCartHandlerTest {

    private final CartRepository repository = mock(CartRepository.class);
    private final OutboxStore outboxStore = mock(OutboxStore.class);
    private ClearCartHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ClearCartHandler(repository, outboxStore);
        when(repository.save(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void handle_clearsItemsSavesAndAppendsSagaCartClearedToOutboxKeyedByGuestToken() throws Exception {
        UUID token = UUID.randomUUID();
        ShoppingCart cart = ShoppingCart.create(new CartId(token), new GuestToken(token.toString()));
        cart.addItem(new ProductId(UUID.randomUUID()), new Quantity(2));
        when(repository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(cart)));

        handler.handle(new ClearCartCommand(token.toString())).get();

        assertThat(cart.getItems()).isEmpty();
        verify(repository).save(cart);
        // CLAUDE.md §5 #5: handler constructs a fresh CartCleared (bypasses cart.getUncommittedEvents).
        // Outbox aggregate key is the guestToken (not the cartId) — preserves the bypass behavior.
        ArgumentCaptor<List<DomainEvent>> events = ArgumentCaptor.forClass(List.class);
        verify(outboxStore).append(eq(token.toString()), events.capture());
        assertThat(events.getValue()).singleElement()
                .isInstanceOfSatisfying(CartCleared.class,
                        e -> assertThat(e.guestToken()).isEqualTo(token.toString()));
    }

    @Test
    void handle_cartMissing_isNoOp() throws Exception {
        when(repository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        handler.handle(new ClearCartCommand(UUID.randomUUID().toString())).get();

        verify(repository, never()).save(any());
        verify(outboxStore, never()).append(any(), any());
    }
}
