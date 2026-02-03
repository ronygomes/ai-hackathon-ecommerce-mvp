package me.ronygomes.ecommerce.productcatalog.process.commandapi;

import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.infrastructure.RabbitMQCommandBus;
import me.ronygomes.ecommerce.productcatalog.application.CreateProductCommand;
import tools.jackson.databind.ObjectMapper;

import io.javalin.Javalin;
import tools.jackson.databind.ObjectMapper;

public class CommandApi {
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
        }).start(8080);

        CommandBus commandBus = new RabbitMQCommandBus("product_catalog_commands", "localhost");
        ObjectMapper objectMapper = new ObjectMapper();

        app.post("/products", ctx -> {
            CreateProductCommand command = objectMapper.readValue(ctx.body(), CreateProductCommand.class);
            commandBus.send(command);
            ctx.status(202);
            ctx.result("{\"status\": \"Accepted\"}");
        });

        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(500);
            ctx.result("{\"error\": \"" + e.getMessage() + "\"}");
        });
    }
}
