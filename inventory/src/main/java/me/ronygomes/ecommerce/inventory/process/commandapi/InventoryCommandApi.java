package me.ronygomes.ecommerce.inventory.process.commandapi;

import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.infrastructure.RabbitMQCommandBus;
import me.ronygomes.ecommerce.inventory.application.SetStockCommand;
import me.rongyomes.ecommerce.checkout.saga.message.command.DeductStockForOrderCommand;
import tools.jackson.databind.ObjectMapper;

import static spark.Spark.*;

public class InventoryCommandApi {
    public static void main(String[] args) {
        port(8082);

        CommandBus commandBus = new RabbitMQCommandBus("inventory_commands", "localhost");
        ObjectMapper objectMapper = new ObjectMapper();

        // Admin: Set Stock
        post("/inventory/stock", (req, res) -> {
            SetStockCommand command = objectMapper.readValue(req.body(), SetStockCommand.class);
            commandBus.send(command).get(); // Sync for admin simplicity in MVP
            res.status(202);
            return "{\"status\": \"Accepted\"}";
        });

        // System: Deduct Stock for Order
        post("/inventory/deductions", (req, res) -> {
            DeductStockForOrderCommand command = objectMapper.readValue(req.body(), DeductStockForOrderCommand.class);
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
