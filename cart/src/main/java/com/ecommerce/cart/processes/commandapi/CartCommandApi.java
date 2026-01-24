package com.ecommerce.cart.processes.commandapi;

import com.ecommerce.core.application.ICommandBus;
import com.ecommerce.core.infrastructure.RabbitMQCommandBus;
import com.ecommerce.cart.application.*;
import tools.jackson.databind.ObjectMapper;

import static spark.Spark.*;

public class CartCommandApi {
    public static void main(String[] args) {
        port(8084);

        ICommandBus commandBus = new RabbitMQCommandBus("cart_commands", "localhost");
        ObjectMapper objectMapper = new ObjectMapper();

        post("/cart/items", (req, res) -> {
            AddCartItemCommand command = objectMapper.readValue(req.body(), AddCartItemCommand.class);
            commandBus.send(command).get(); // Sync for simplicity in MVP
            res.status(202);
            return "{\"status\": \"Accepted\"}";
        });

        put("/cart/items", (req, res) -> {
            UpdateCartItemQtyCommand command = objectMapper.readValue(req.body(), UpdateCartItemQtyCommand.class);
            commandBus.send(command).get();
            res.status(202);
            return "{\"status\": \"Accepted\"}";
        });

        delete("/cart/items/:productId", (req, res) -> {
            String guestToken = req.headers("X-Guest-Token");
            if (guestToken == null || guestToken.isBlank()) {
                res.status(400);
                return "{\"error\": \"X-Guest-Token header missing\"}";
            }
            RemoveCartItemCommand command = new RemoveCartItemCommand(guestToken,
                    java.util.UUID.fromString(req.params(":productId")));
            commandBus.send(command).get();
            res.status(202);
            return "{\"status\": \"Accepted\"}";
        });

        delete("/cart", (req, res) -> {
            String guestToken = req.headers("X-Guest-Token");
            if (guestToken == null || guestToken.isBlank()) {
                res.status(400);
                return "{\"error\": \"X-Guest-Token header missing\"}";
            }
            ClearCartCommand command = new ClearCartCommand(guestToken);
            commandBus.send(command).get();
            res.status(202);
            return "{\"status\": \"Accepted\"}";
        });

        exception(Exception.class, (e, req, res) -> {
            res.status(500);
            res.body("{\"error\": \"" + e.getMessage() + "\"}");
        });
    }
}
