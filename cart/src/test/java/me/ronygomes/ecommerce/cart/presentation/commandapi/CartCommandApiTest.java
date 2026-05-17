package me.ronygomes.ecommerce.cart.presentation.commandapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.testtools.JavalinTest;
import me.rongyomes.ecommerce.checkout.saga.message.command.ClearCartCommand;
import me.ronygomes.ecommerce.cart.application.AddCartItemCommand;
import me.ronygomes.ecommerce.cart.application.RemoveCartItemCommand;
import me.ronygomes.ecommerce.cart.application.UpdateCartItemQtyCommand;
import me.ronygomes.ecommerce.core.application.Command;
import me.ronygomes.ecommerce.core.application.CommandBus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartCommandApiTest {

    private final CommandBus commandBus = mock(CommandBus.class);

    private Javalin setupApp() {
        when(commandBus.send(any())).thenReturn(CompletableFuture.completedFuture(null));
        return Javalin.create(config -> CartCommandApi.register(config, commandBus, new ObjectMapper()));
    }

    @Test
    void postItem_dispatchesAddCartItemCommand() {
        UUID productId = UUID.randomUUID();

        JavalinTest.test(setupApp(), (server, client) -> {
            var response = client.post("/cart/guest-1/items",
                    "{\"productId\":\"" + productId + "\",\"qty\":4}");
            assertThat(response.code()).isEqualTo(HttpStatus.ACCEPTED.getCode());

            ArgumentCaptor<Command<?>> captor = ArgumentCaptor.forClass(Command.class);
            verify(commandBus).send(captor.capture());
            assertThat(captor.getValue()).isInstanceOfSatisfying(AddCartItemCommand.class, cmd -> {
                assertThat(cmd.guestToken()).isEqualTo("guest-1");
                assertThat(cmd.productId()).isEqualTo(productId);
                assertThat(cmd.qty()).isEqualTo(4);
            });
        });
    }

    @Test
    void putItem_dispatchesUpdateCartItemQtyCommand() {
        UUID productId = UUID.randomUUID();

        JavalinTest.test(setupApp(), (server, client) -> {
            var response = client.put("/cart/guest-1/items/" + productId, "{\"qty\":9}");
            assertThat(response.code()).isEqualTo(HttpStatus.ACCEPTED.getCode());

            ArgumentCaptor<Command<?>> captor = ArgumentCaptor.forClass(Command.class);
            verify(commandBus).send(captor.capture());
            assertThat(captor.getValue()).isInstanceOfSatisfying(UpdateCartItemQtyCommand.class, cmd -> {
                assertThat(cmd.productId()).isEqualTo(productId);
                assertThat(cmd.qty()).isEqualTo(9);
            });
        });
    }

    @Test
    void deleteItem_dispatchesRemoveCartItemCommand() {
        UUID productId = UUID.randomUUID();

        JavalinTest.test(setupApp(), (server, client) -> {
            var response = client.delete("/cart/guest-1/items/" + productId);
            assertThat(response.code()).isEqualTo(HttpStatus.ACCEPTED.getCode());

            ArgumentCaptor<Command<?>> captor = ArgumentCaptor.forClass(Command.class);
            verify(commandBus).send(captor.capture());
            assertThat(captor.getValue()).isInstanceOfSatisfying(RemoveCartItemCommand.class, cmd ->
                    assertThat(cmd.productId()).isEqualTo(productId));
        });
    }

    @Test
    void deleteCart_dispatchesClearCartCommand() {
        JavalinTest.test(setupApp(), (server, client) -> {
            var response = client.delete("/cart/guest-1");
            assertThat(response.code()).isEqualTo(HttpStatus.ACCEPTED.getCode());

            ArgumentCaptor<Command<?>> captor = ArgumentCaptor.forClass(Command.class);
            verify(commandBus).send(captor.capture());
            assertThat(captor.getValue()).isInstanceOfSatisfying(ClearCartCommand.class, cmd ->
                    assertThat(cmd.guestToken()).isEqualTo("guest-1"));
        });
    }
}
