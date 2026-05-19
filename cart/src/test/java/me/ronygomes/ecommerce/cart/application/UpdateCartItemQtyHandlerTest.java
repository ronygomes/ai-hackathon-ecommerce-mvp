package me.ronygomes.ecommerce.cart.application;

import me.ronygomes.ecommerce.cart.domain.*;
import me.ronygomes.ecommerce.cart.infrastructure.CartRepository;
import me.ronygomes.ecommerce.core.domain.DomainEvent;
import me.ronygomes.ecommerce.core.infrastructure.outbox.OutboxStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UpdateCartItemQtyHandlerTest {

    private final CartRepository repository = mock(CartRepository.class);
    private final OutboxStore outboxStore = mock(OutboxStore.class);
    private UpdateCartItemQtyHandler handler;

    @BeforeEach
    void setUp() {
        handler = new UpdateCartItemQtyHandler(repository, outboxStore);
        when(repository.save(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void handle_changesQuantityInPlaceAndPushesCartItemQuantityUpdated() throws Exception {
        ShoppingCart cart = ShoppingCart.create(CartId.generate(), new GuestToken("g1"));
        UUID productId = UUID.randomUUID();
        cart.addItem(new ProductId(productId), new Quantity(2));
        cart.clearUncommittedEvents(); // discard CartCreated + CartItemAdded from setup
        when(repository.getByGuestToken(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(cart)));
        AtomicReference<List<DomainEvent>> appended = snapshotAppendedEvents();

        handler.handle(new UpdateCartItemQtyCommand("g1", productId, 9)).get();

        assertThat(cart.getItems()).singleElement()
                .satisfies(item -> assertThat(item.getQuantity().value()).isEqualTo(9));
        verify(repository).save(cart);
        assertThat(appended.get()).singleElement()
                .isInstanceOfSatisfying(CartItemQuantityUpdated.class, e -> {
                    assertThat(e.productId()).isEqualTo(productId);
                    assertThat(e.oldQty()).isEqualTo(2);
                    assertThat(e.newQty()).isEqualTo(9);
                });
    }

    @Test
    void handle_cartMissing_isNoOpAndDoesNotSaveOrAppend() throws Exception {
        when(repository.getByGuestToken(any())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        handler.handle(new UpdateCartItemQtyCommand("g1", UUID.randomUUID(), 1)).get();

        verify(repository, never()).save(any());
        verify(outboxStore, never()).append(any(), any());
    }

    private AtomicReference<List<DomainEvent>> snapshotAppendedEvents() {
        AtomicReference<List<DomainEvent>> ref = new AtomicReference<>(List.of());
        doAnswer(inv -> {
            ref.set(new ArrayList<>(inv.getArgument(1)));
            return null;
        }).when(outboxStore).append(any(), any());
        return ref;
    }
}
