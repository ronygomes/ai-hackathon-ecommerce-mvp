package com.ecommerce.productcatalog.processes.commandapi;

import com.ecommerce.core.application.ICommandBus;
import com.ecommerce.core.infrastructure.RabbitMQCommandBus;
import com.ecommerce.productcatalog.application.CreateProductCommand;
import tools.jackson.databind.ObjectMapper;

import static spark.Spark.*;

public class CommandApi {
    public static void main(String[] args) {
        port(8080);

        ICommandBus commandBus = new RabbitMQCommandBus("product_catalog_commands", "localhost");
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
