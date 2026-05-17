package me.ronygomes.ecommerce.cart.presentation.commandapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.HttpStatus;
import me.rongyomes.ecommerce.checkout.saga.message.command.ClearCartCommand;
import me.ronygomes.ecommerce.cart.application.AddCartItemCommand;
import me.ronygomes.ecommerce.cart.application.RemoveCartItemCommand;
import me.ronygomes.ecommerce.cart.application.UpdateCartItemQtyCommand;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.infrastructure.RabbitMQCommandBus;

import java.util.UUID;

public class CartCommandApi {

    public static void register(JavalinConfig config, CommandBus commandBus, ObjectMapper objectMapper) {

        config.routes.post("/cart/{guestToken}/items", ctx -> {
            String guestToken = ctx.pathParam("guestToken");
            AddItemToCartRequest body = objectMapper.readValue(ctx.body(), AddItemToCartRequest.class);
            commandBus.send(new AddCartItemCommand(guestToken, UUID.fromString(body.productId()), body.qty()));
            ctx.status(HttpStatus.ACCEPTED);
            ctx.result("{}");
        });

        config.routes.put("/cart/{guestToken}/items/{productId}", ctx -> {
            String guestToken = ctx.pathParam("guestToken");
            String productId = ctx.pathParam("productId");
            UpdateCartItemRequest body = objectMapper.readValue(ctx.body(), UpdateCartItemRequest.class);
            commandBus.send(new UpdateCartItemQtyCommand(guestToken, UUID.fromString(productId), body.qty()));
            ctx.status(HttpStatus.ACCEPTED);
            ctx.result("{}");
        });

        config.routes.delete("/cart/{guestToken}/items/{productId}", ctx -> {
            String guestToken = ctx.pathParam("guestToken");
            String productId = ctx.pathParam("productId");
            commandBus.send(new RemoveCartItemCommand(guestToken, UUID.fromString(productId)));
            ctx.status(HttpStatus.ACCEPTED);
            ctx.result("{}");
        });

        config.routes.delete("/cart/{guestToken}", ctx -> {
            String guestToken = ctx.pathParam("guestToken");
            commandBus.send(new ClearCartCommand(guestToken));
            ctx.status(HttpStatus.ACCEPTED);
            ctx.result("{}");
        });
    }

    static void main() {
        CommandBus commandBus = new RabbitMQCommandBus("cart_commands", "localhost");
        ObjectMapper objectMapper = new ObjectMapper();

        Javalin.create(config -> register(config, commandBus, objectMapper)).start(8084);
    }

    public record AddItemToCartRequest(String productId, int qty) {
    }

    public record UpdateCartItemRequest(int qty) {
    }
}
