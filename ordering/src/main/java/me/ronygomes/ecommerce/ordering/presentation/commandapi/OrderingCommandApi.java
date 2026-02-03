package me.ronygomes.ecommerce.ordering.presentation.commandapi;

import io.javalin.Javalin;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.infrastructure.RabbitMQCommandBus;
import me.ronygomes.ecommerce.ordering.application.PlaceOrderCommand;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OrderingCommandApi {
    static void main() {
        Javalin app = Javalin.create(config -> {
        }).start(8086);

        CommandBus commandBus = new RabbitMQCommandBus("ordering_commands", "localhost");
        ObjectMapper objectMapper = new ObjectMapper();

        app.post("/orders", ctx -> {
            PlaceOrderCommand command = objectMapper.readValue(ctx.body(), PlaceOrderCommand.class);
            java.util.UUID orderId = commandBus.send(command).get();
            ctx.status(202);
            ctx.result("{\"orderId\": \"" + orderId.toString() + "\", \"status\": \"Accepted\"}");
        });

        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(500);
            ctx.result("{\"error\": \"" + e.getMessage() + "\"}");
        });
    }
}
