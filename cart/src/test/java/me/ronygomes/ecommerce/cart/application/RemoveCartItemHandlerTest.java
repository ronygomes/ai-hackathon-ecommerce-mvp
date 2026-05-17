package me.ronygomes.ecommerce.cart.application;

import me.ronygomes.ecommerce.cart.domain.CartId;
import me.ronygomes.ecommerce.cart.domain.GuestToken;
import me.ronygomes.ecommerce.cart.domain.ProductId;
import me.ronygomes.ecommerce.cart.domain.Quantity;
import me.ronygomes.ecommerce.cart.domain.ShoppingCart;
import me.ronygomes.ecommerce.cart.infrastructure.CartRepository;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RemoveCartItemHandlerTest {

    private final CartRepository repository = mock(CartRepository.class);
    private final MessageBus messageBus = mock(MessageBus.class);
    private RemoveCartItemHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RemoveCartItemHandler(repository, messageBus);
        when(repository.save(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(messageBus.publish(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void handle_dropsMatchingItemAndSaves() throws Exception {
        ShoppingCart cart = ShoppingCart.create(CartId.generate(), new GuestToken("g1"));
        UUID productId = UUID.randomUUID();
        cart.addItem(new ProductId(productId), new Quantity(2));
        cart.addItem(new ProductId(UUID.randomUUID()), new Quantity(1));
        when(repository.getByGuestToken(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(cart)));

        handler.handle(new RemoveCartItemCommand("g1", productId)).get();

        assertThat(cart.getItems()).hasSize(1)
                .allSatisfy(item -> assertThat(item.getProductId().value()).isNotEqualTo(productId));
        verify(repository).save(cart);
    }

    @Test
    void handle_cartMissing_isNoOp() throws Exception {
        when(repository.getByGuestToken(any())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        handler.handle(new RemoveCartItemCommand("g1", UUID.randomUUID())).get();

        verify(repository, never()).save(any());
    }
}
