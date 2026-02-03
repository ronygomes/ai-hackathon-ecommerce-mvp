package me.ronygomes.ecommerce.productcatalog.process.commandapi;

import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.infrastructure.RabbitMQCommandBus;
import me.ronygomes.ecommerce.productcatalog.application.CreateProductCommand;
import tools.jackson.databind.ObjectMapper;

import static spark.Spark.*;

public class CommandApi {
    public static void main(String[] args) {
        port(8080);

        CommandBus commandBus = new RabbitMQCommandBus("product_catalog_commands", "localhost");
        ObjectMapper objectMapper = new ObjectMapper();

        post("/products", (req, res) -> {
            CreateProductCommand command = objectMapper.readValue(req.body(), CreateProductCommand.class);
            commandBus.send(command);
            res.status(202);
            return "{\"status\": \"Accepted\"}";
        });

        exception(Exception.class, (e, req, res) -> {
            res.status(500);
            res.body("{\"error\": \"" + e.getMessage() + "\"}");
        });
    }
}
