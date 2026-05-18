package me.ronygomes.ecommerce.inventory.presentation.commandapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.HttpStatus;
import me.ronygomes.ecommerce.checkout.saga.message.command.DeductStockForOrderCommand;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.infrastructure.RabbitMQCommandBus;
import me.ronygomes.ecommerce.inventory.application.SetStockCommand;

public class InventoryCommandApi {

    public static void register(JavalinConfig config, CommandBus commandBus, ObjectMapper objectMapper) {

        config.routes.post("/inventory/stock", ctx -> {
            SetStockCommand command = objectMapper.readValue(ctx.body(), SetStockCommand.class);
            commandBus.send(command);
            ctx.status(HttpStatus.ACCEPTED);
            ctx.result("{\"status\": \"Accepted\"}");
        });

        config.routes.post("/inventory/deductions", ctx -> {
            DeductStockForOrderCommand command = objectMapper.readValue(ctx.body(), DeductStockForOrderCommand.class);
            commandBus.send(command);
            ctx.status(HttpStatus.ACCEPTED);
            ctx.result("{\"status\": \"Accepted\"}");
        });

        config.routes.exception(Exception.class, (e, ctx) -> {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.result("{\"error\": \"" + e.getMessage() + "\"}");
        });
    }

    static void main() {
        CommandBus commandBus = new RabbitMQCommandBus("inventory_commands", "localhost");
        ObjectMapper objectMapper = new ObjectMapper();

        Javalin.create(config -> register(config, commandBus, objectMapper)).start(8082);
    }
}
