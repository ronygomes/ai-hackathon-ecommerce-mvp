package me.ronygomes.ecommerce.inventory.presentation.commandapi;

import io.javalin.Javalin;
import me.rongyomes.ecommerce.checkout.saga.message.command.DeductStockForOrderCommand;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.infrastructure.RabbitMQCommandBus;
import me.ronygomes.ecommerce.inventory.application.SetStockCommand;
import com.fasterxml.jackson.databind.ObjectMapper;

public class InventoryCommandApi {
    static void main() {
        Javalin app = Javalin.create(config -> {
        }).start(8082);

        CommandBus commandBus = new RabbitMQCommandBus("inventory_commands", "localhost");
        ObjectMapper objectMapper = new ObjectMapper();

        // Admin: Set Stock
        app.post("/inventory/stock", ctx -> {
            SetStockCommand command = objectMapper.readValue(ctx.body(), SetStockCommand.class);
            commandBus.send(command).get(); // Sync for admin simplicity in MVP
            ctx.status(202);
            ctx.result("{\"status\": \"Accepted\"}");
        });

        // System: Deduct Stock for Order
        app.post("/inventory/deductions", ctx -> {
            DeductStockForOrderCommand command = objectMapper.readValue(ctx.body(), DeductStockForOrderCommand.class);
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
