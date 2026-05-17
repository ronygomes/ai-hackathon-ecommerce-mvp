package me.ronygomes.ecommerce.cart.application;

import me.ronygomes.ecommerce.cart.domain.CartId;
import me.ronygomes.ecommerce.cart.domain.GuestToken;
import me.ronygomes.ecommerce.cart.domain.ShoppingCart;
import me.ronygomes.ecommerce.cart.infrastructure.CartRepository;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AddCartItemHandlerTest {

    private final CartRepository repository = mock(CartRepository.class);
    private final MessageBus messageBus = mock(MessageBus.class);
    private AddCartItemHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AddCartItemHandler(repository, messageBus);
        when(repository.save(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void handle_existingCart_appendsItemToSavedAggregate() throws Exception {
        UUID token = UUID.randomUUID();
        ShoppingCart existing = ShoppingCart.create(new CartId(token), new GuestToken(token.toString()));
        when(repository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(existing)));
        UUID productId = UUID.randomUUID();

        handler.handle(new AddCartItemCommand(token.toString(), productId, 3)).get();

        assertThat(existing.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getProductId().value()).isEqualTo(productId);
            assertThat(item.getQuantity().value()).isEqualTo(3);
        });
        verify(repository).save(existing);
    }

    @Test
    void handle_missingCart_createsNewOneSavesAndAddsItem() throws Exception {
        UUID token = UUID.randomUUID();
        when(repository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        handler.handle(new AddCartItemCommand(token.toString(), UUID.randomUUID(), 2)).get();

        ArgumentCaptor<ShoppingCart> saved = ArgumentCaptor.forClass(ShoppingCart.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getId().value()).isEqualTo(token);
        assertThat(saved.getValue().getItems()).hasSize(1);
    }

    @Test
    void handle_doesNotPublishAnyEvents_currentBuggyBehavior() throws Exception {
        UUID token = UUID.randomUUID();
        when(repository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        handler.handle(new AddCartItemCommand(token.toString(), UUID.randomUUID(), 1)).get();

        verify(messageBus, never()).publish(any());
    }
}
