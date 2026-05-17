package me.ronygomes.ecommerce.inventory.presentation.commandapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.testtools.JavalinTest;
import me.rongyomes.ecommerce.checkout.saga.message.command.DeductStockForOrderCommand;
import me.ronygomes.ecommerce.core.application.Command;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.inventory.application.SetStockCommand;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryCommandApiTest {

    private final CommandBus commandBus = mock(CommandBus.class);

    private Javalin setupApp() {
        return Javalin.create(config -> InventoryCommandApi.register(config, commandBus, new ObjectMapper()));
    }

    @Test
    void postStock_dispatchesSetStockCommandAndReturns202() {
        when(commandBus.send(any())).thenReturn(CompletableFuture.completedFuture(null));
        UUID pid = UUID.randomUUID();
        String body = "{\"productId\":\"" + pid + "\",\"newQty\":15,\"reason\":\"restock\"}";

        JavalinTest.test(setupApp(), (server, client) -> {
            var response = client.post("/inventory/stock", body);
            assertThat(response.code()).isEqualTo(HttpStatus.ACCEPTED.getCode());

            ArgumentCaptor<Command<?>> captor = ArgumentCaptor.forClass(Command.class);
            verify(commandBus).send(captor.capture());
            assertThat(captor.getValue()).isInstanceOfSatisfying(SetStockCommand.class, cmd -> {
                assertThat(cmd.productId()).isEqualTo(pid);
                assertThat(cmd.newQty()).isEqualTo(15);
                assertThat(cmd.reason()).isEqualTo("restock");
            });
        });
    }

    @Test
    void postDeductions_dispatchesDeductStockForOrderCommandAndReturns202() {
        when(commandBus.send(any())).thenReturn(CompletableFuture.completedFuture(null));
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String body = "{\"orderId\":\"" + orderId + "\",\"items\":[{\"productId\":\"" + productId + "\",\"qty\":3}]}";

        JavalinTest.test(setupApp(), (server, client) -> {
            var response = client.post("/inventory/deductions", body);
            assertThat(response.code()).isEqualTo(HttpStatus.ACCEPTED.getCode());

            ArgumentCaptor<Command<?>> captor = ArgumentCaptor.forClass(Command.class);
            verify(commandBus).send(captor.capture());
            assertThat(captor.getValue()).isInstanceOfSatisfying(DeductStockForOrderCommand.class, cmd -> {
                assertThat(cmd.orderId()).isEqualTo(orderId);
                assertThat(cmd.items()).singleElement().satisfies(item -> {
                    assertThat(item.productId()).isEqualTo(productId);
                    assertThat(item.qty()).isEqualTo(3);
                });
            });
        });
    }

    @Test
    void postStock_withMalformedJson_returns500ViaExceptionHandler() {
        JavalinTest.test(setupApp(), (server, client) -> {
            var response = client.post("/inventory/stock", "{ not json");
            assertThat(response.code()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
        });
    }
}
