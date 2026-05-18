package me.ronygomes.ecommerce.cart.application;

import me.ronygomes.ecommerce.cart.domain.CartId;
import me.ronygomes.ecommerce.cart.domain.CartItemRemoved;
import me.ronygomes.ecommerce.cart.domain.GuestToken;
import me.ronygomes.ecommerce.cart.domain.ProductId;
import me.ronygomes.ecommerce.cart.domain.Quantity;
import me.ronygomes.ecommerce.cart.domain.ShoppingCart;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RemoveCartItemHandlerTest {

    private final CartRepository repository = mock(CartRepository.class);
    private final OutboxStore outboxStore = mock(OutboxStore.class);
    private RemoveCartItemHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RemoveCartItemHandler(repository, outboxStore);
        when(repository.save(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void handle_dropsMatchingItemAndPushesCartItemRemoved() throws Exception {
        ShoppingCart cart = ShoppingCart.create(CartId.generate(), new GuestToken("g1"));
        UUID productId = UUID.randomUUID();
        cart.addItem(new ProductId(productId), new Quantity(2));
        cart.addItem(new ProductId(UUID.randomUUID()), new Quantity(1));
        cart.clearUncommittedEvents(); // discard setup events
        when(repository.getByGuestToken(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(cart)));
        AtomicReference<List<DomainEvent>> appended = snapshotAppendedEvents();

        handler.handle(new RemoveCartItemCommand("g1", productId)).get();

        assertThat(cart.getItems()).hasSize(1)
                .allSatisfy(item -> assertThat(item.getProductId().value()).isNotEqualTo(productId));
        verify(repository).save(cart);
        assertThat(appended.get()).singleElement()
                .isInstanceOfSatisfying(CartItemRemoved.class, e -> {
                    assertThat(e.cartId()).isEqualTo(cart.getId().value());
                    assertThat(e.productId()).isEqualTo(productId);
                });
    }

    @Test
    void handle_cartMissing_isNoOp() throws Exception {
        when(repository.getByGuestToken(any())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        handler.handle(new RemoveCartItemCommand("g1", UUID.randomUUID())).get();

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
