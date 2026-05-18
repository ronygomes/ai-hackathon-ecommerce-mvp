package me.ronygomes.ecommerce.ordering.presentation.commandapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.HttpStatus;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.infrastructure.AppConfig;
import me.ronygomes.ecommerce.core.infrastructure.RabbitMQCommandBus;
import me.ronygomes.ecommerce.ordering.application.PlaceOrderCommand;
import me.ronygomes.ecommerce.ordering.domain.CustomerInfo;
import me.ronygomes.ecommerce.ordering.domain.ShippingAddress;

import java.util.List;
import java.util.UUID;

public class OrderingCommandApi {

    public static void register(JavalinConfig config, CommandBus commandBus, ObjectMapper objectMapper) {

        config.routes.post("/orders", ctx -> {
            PlaceOrderRequest body = objectMapper.readValue(ctx.body(), PlaceOrderRequest.class);
            UUID orderId = UUID.nameUUIDFromBytes(body.idempotencyKey().getBytes());
            PlaceOrderCommand command = new PlaceOrderCommand(
                    orderId,
                    body.guestToken(),
                    body.cartId(),
                    body.customerInfo(),
                    body.address(),
                    body.idempotencyKey(),
                    body.items());

            commandBus.send(command);

            ctx.status(HttpStatus.ACCEPTED);
            ctx.contentType("application/json");
            ctx.result("{\"orderId\": \"" + orderId + "\", \"status\": \"Accepted\"}");
        });

        config.routes.exception(Exception.class, (e, ctx) -> {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.result("{\"error\": \"" + e.getMessage() + "\"}");
        });
    }

    public record PlaceOrderRequest(
            String guestToken,
            String cartId,
            CustomerInfo customerInfo,
            ShippingAddress address,
            String idempotencyKey,
            List<PlaceOrderCommand.OrderItemRequest> items) {
    }

    static void main() {
        AppConfig appConfig = AppConfig.fromEnv();
        CommandBus commandBus = new RabbitMQCommandBus("ordering_commands", appConfig.rabbitHost());
        ObjectMapper objectMapper = new ObjectMapper();

        Javalin.create(cfg -> register(cfg, commandBus, objectMapper)).start(8086);
    }
}
