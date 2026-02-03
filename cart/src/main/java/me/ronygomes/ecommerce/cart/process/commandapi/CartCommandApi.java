package me.ronygomes.ecommerce.cart.process.commandapi;

import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.infrastructure.RabbitMQCommandBus;
import me.rongyomes.ecommerce.checkout.saga.message.command.ClearCartCommand;
import me.ronygomes.ecommerce.cart.application.AddCartItemCommand;
import me.ronygomes.ecommerce.cart.application.RemoveCartItemCommand;
import me.ronygomes.ecommerce.cart.application.UpdateCartItemQtyCommand;
import tools.jackson.databind.ObjectMapper;
import static spark.Spark.*;
import java.util.UUID;

public class CartCommandApi {
    public static void main(String[] args) {
        port(8083);

        CommandBus commandBus = new RabbitMQCommandBus("cart_commands", "localhost");
        ObjectMapper objectMapper = new ObjectMapper();

        post("/cart/:guestToken/items", (req, res) -> {
            String guestToken = req.params(":guestToken");
            AddItemToCartRequest body = objectMapper.readValue(req.body(), AddItemToCartRequest.class);
            commandBus.send(new AddCartItemCommand(guestToken, UUID.fromString(body.productId()), body.qty()));
            res.status(202);
            return "{}";
        });

        put("/cart/:guestToken/items/:productId", (req, res) -> {
            String guestToken = req.params(":guestToken");
            String productId = req.params(":productId");
            UpdateCartItemRequest body = objectMapper.readValue(req.body(), UpdateCartItemRequest.class);
            commandBus.send(new UpdateCartItemQtyCommand(guestToken, UUID.fromString(productId), body.qty()));
            res.status(202);
            return "{}";
        });

        delete("/cart/:guestToken/items/:productId", (req, res) -> {
            String guestToken = req.params(":guestToken");
            String productId = req.params(":productId");
            commandBus.send(new RemoveCartItemCommand(guestToken, UUID.fromString(productId)));
            res.status(202);
            return "{}";
        });

        delete("/cart/:guestToken", (req, res) -> {
            String guestToken = req.params(":guestToken");
            commandBus.send(new ClearCartCommand(guestToken));
            res.status(202);
            return "{}";
        });
    }

    public record AddItemToCartRequest(String productId, int qty) {
    }

    public record UpdateCartItemRequest(int qty) {
    }
}
