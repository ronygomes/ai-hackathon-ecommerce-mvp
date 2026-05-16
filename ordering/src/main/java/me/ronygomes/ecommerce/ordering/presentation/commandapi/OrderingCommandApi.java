package me.ronygomes.ecommerce.ordering.presentation.commandapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.infrastructure.RabbitMQCommandBus;
import me.ronygomes.ecommerce.ordering.application.PlaceOrderCommand;

public class OrderingCommandApi {
    static void main() {
        CommandBus commandBus = new RabbitMQCommandBus("ordering_commands", "localhost");
        ObjectMapper objectMapper = new ObjectMapper();

        Javalin.create(config -> {
            config.routes.post("/orders", ctx -> {
                PlaceOrderCommand command = objectMapper.readValue(ctx.body(), PlaceOrderCommand.class);
                java.util.UUID orderId = commandBus.send(command).get();
                ctx.status(HttpStatus.ACCEPTED);
                ctx.result("{\"orderId\": \"" + orderId.toString() + "\", \"status\": \"Accepted\"}");
            });

            config.routes.exception(Exception.class, (e, ctx) -> {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                ctx.result("{\"error\": \"" + e.getMessage() + "\"}");
            });
        }).start(8086);
    }
}
