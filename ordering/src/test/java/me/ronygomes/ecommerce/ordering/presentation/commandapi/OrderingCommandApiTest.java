package me.ronygomes.ecommerce.ordering.presentation.commandapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.testtools.JavalinTest;
import me.ronygomes.ecommerce.core.application.Command;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.ordering.application.PlaceOrderCommand;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderingCommandApiTest {

    private final CommandBus commandBus = mock(CommandBus.class);

    private Javalin setupApp() {
        return Javalin.create(config -> OrderingCommandApi.register(config, commandBus, new ObjectMapper()));
    }

    @Test
    void postOrders_derivesOrderIdFromIdempotencyKeyAndEchoesIt() {
        when(commandBus.send(any())).thenReturn(CompletableFuture.completedFuture(null));

        UUID idempotencyKey = UUID.randomUUID();
        UUID expectedOrderId = UUID.nameUUIDFromBytes(idempotencyKey.toString().getBytes());

        String body = """
                {
                  "guestToken": "g1",
                  "cartId": "g1",
                  "customerInfo": {"name": "Jane", "phone": "+1", "email": "j@e"},
                  "address": {"line1": "L", "city": "C", "postalCode": "x", "country": "US"},
                  "idempotencyKey": "%s",
                  "items": [{"productId": "%s", "sku": "S", "name": "N", "unitPrice": 1.0, "qty": 1}]
                }
                """.formatted(idempotencyKey, UUID.randomUUID());

        JavalinTest.test(setupApp(), (server, client) -> {
            var response = client.post("/orders", body);
            assertThat(response.code()).isEqualTo(HttpStatus.ACCEPTED.getCode());
            String responseBody = response.body().string();
            assertThat(responseBody).contains("\"orderId\": \"" + expectedOrderId + "\"")
                    .contains("\"status\": \"Accepted\"");

            ArgumentCaptor<Command<?>> captor = ArgumentCaptor.forClass(Command.class);
            verify(commandBus).send(captor.capture());
            assertThat(captor.getValue()).isInstanceOfSatisfying(PlaceOrderCommand.class, cmd -> {
                assertThat(cmd.orderId()).isEqualTo(expectedOrderId);
                assertThat(cmd.guestToken()).isEqualTo("g1");
                assertThat(cmd.idempotencyKey()).isEqualTo(idempotencyKey.toString());
            });
        });
    }

    @Test
    void postOrders_sameIdempotencyKey_producesSameOrderIdOnRetry() {
        when(commandBus.send(any())).thenReturn(CompletableFuture.completedFuture(null));

        UUID idempotencyKey = UUID.randomUUID();
        String body = """
                {
                  "guestToken": "g1",
                  "cartId": "g1",
                  "customerInfo": {"name": "Jane", "phone": "+1", "email": "j@e"},
                  "address": {"line1": "L", "city": "C", "postalCode": "x", "country": "US"},
                  "idempotencyKey": "%s",
                  "items": [{"productId": "%s", "sku": "S", "name": "N", "unitPrice": 1.0, "qty": 1}]
                }
                """.formatted(idempotencyKey, UUID.randomUUID());

        JavalinTest.test(setupApp(), (server, client) -> {
            String first = client.post("/orders", body).body().string();
            String second = client.post("/orders", body).body().string();
            assertThat(first).isEqualTo(second);
        });
    }

    @Test
    void postOrders_withMalformedJson_returns500ViaExceptionHandler() {
        JavalinTest.test(setupApp(), (server, client) -> {
            var response = client.post("/orders", "{ not json");
            assertThat(response.code()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
        });
    }
}
