package me.ronygomes.ecommerce.cart.application;

import me.ronygomes.ecommerce.cart.domain.CartId;
import me.ronygomes.ecommerce.cart.domain.ShoppingCart;
import me.ronygomes.ecommerce.cart.infrastructure.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateCartHandlerTest {

    private final CartRepository repository = mock(CartRepository.class);
    private CreateCartHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CreateCartHandler(repository);
        when(repository.save(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void handle_savesCartKeyedByGuestTokenDerivedCartId() throws Exception {
        UUID token = UUID.randomUUID();

        handler.handle(new CreateCartCommand(token.toString())).get();

        ArgumentCaptor<ShoppingCart> saved = ArgumentCaptor.forClass(ShoppingCart.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo(CartId.fromGuestToken(token.toString()));
        assertThat(saved.getValue().getGuestToken().value()).isEqualTo(token.toString());
    }

    @Test
    void handle_nonUuidGuestToken_succeedsWithDerivedCartId() throws Exception {
        handler.handle(new CreateCartCommand("not-a-uuid")).get();

        ArgumentCaptor<ShoppingCart> saved = ArgumentCaptor.forClass(ShoppingCart.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo(CartId.fromGuestToken("not-a-uuid"));
        assertThat(saved.getValue().getGuestToken().value()).isEqualTo("not-a-uuid");
    }
}
