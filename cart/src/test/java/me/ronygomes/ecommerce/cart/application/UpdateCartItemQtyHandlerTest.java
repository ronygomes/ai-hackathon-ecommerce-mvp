package me.ronygomes.ecommerce.cart.application;

import me.ronygomes.ecommerce.cart.domain.CartId;
import me.ronygomes.ecommerce.cart.domain.GuestToken;
import me.ronygomes.ecommerce.cart.domain.ProductId;
import me.ronygomes.ecommerce.cart.domain.Quantity;
import me.ronygomes.ecommerce.cart.domain.ShoppingCart;
import me.ronygomes.ecommerce.cart.infrastructure.CartRepository;
import me.ronygomes.ecommerce.core.infrastructure.outbox.OutboxStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    void handle_changesQuantityInPlaceAndSavesCartAndAppendsEmptyToOutbox() throws Exception {
        ShoppingCart cart = ShoppingCart.create(CartId.generate(), new GuestToken("g1"));
        UUID productId = UUID.randomUUID();
        cart.addItem(new ProductId(productId), new Quantity(2));
        when(repository.getByGuestToken(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(cart)));

        handler.handle(new UpdateCartItemQtyCommand("g1", productId, 9)).get();

        assertThat(cart.getItems()).singleElement()
                .satisfies(item -> assertThat(item.getQuantity().value()).isEqualTo(9));
        verify(repository).save(cart);
        // CLAUDE.md §5 #2: changeQuantity emits no event; append is called with empty list.
        verify(outboxStore).append(eq(cart.getId().toString()), eq(List.of()));
    }

    @Test
    void handle_cartMissing_isNoOpAndDoesNotSaveOrAppend() throws Exception {
        when(repository.getByGuestToken(any())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        handler.handle(new UpdateCartItemQtyCommand("g1", UUID.randomUUID(), 1)).get();

        verify(repository, never()).save(any());
        verify(outboxStore, never()).append(any(), any());
    }
}
