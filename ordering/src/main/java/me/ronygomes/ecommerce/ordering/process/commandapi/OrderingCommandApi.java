package me.ronygomes.ecommerce.ordering.process.commandapi;

import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.infrastructure.RabbitMQCommandBus;
import me.ronygomes.ecommerce.ordering.application.PlaceOrderCommand;
import tools.jackson.databind.ObjectMapper;

import io.javalin.Javalin;
import tools.jackson.databind.ObjectMapper;

public class OrderingCommandApi {
    public static void main(String[] args) {
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
