package me.ronygomes.ecommerce.ordering.process.commandapi;

import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.infrastructure.RabbitMQCommandBus;
import me.ronygomes.ecommerce.ordering.application.PlaceOrderCommand;
import tools.jackson.databind.ObjectMapper;

import static spark.Spark.*;

public class OrderingCommandApi {
    public static void main(String[] args) {
        port(8086);

        CommandBus commandBus = new RabbitMQCommandBus("ordering_commands", "localhost");
        ObjectMapper objectMapper = new ObjectMapper();

        post("/orders", (req, res) -> {
            PlaceOrderCommand command = objectMapper.readValue(req.body(), PlaceOrderCommand.class);
            java.util.UUID orderId = commandBus.send(command).get();
            res.status(202);
            return "{\"orderId\": \"" + orderId.toString() + "\", \"status\": \"Accepted\"}";
        });

        exception(Exception.class, (e, req, res) -> {
            res.status(500);
            res.body("{\"error\": \"" + e.getMessage() + "\"}");
        });
    }
}
