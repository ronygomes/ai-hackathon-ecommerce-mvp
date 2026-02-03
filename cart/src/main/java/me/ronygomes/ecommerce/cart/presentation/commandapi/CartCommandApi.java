package me.ronygomes.ecommerce.cart.presentation.commandapi;

import io.javalin.Javalin;
import me.rongyomes.ecommerce.checkout.saga.message.command.ClearCartCommand;
import me.ronygomes.ecommerce.cart.application.AddCartItemCommand;
import me.ronygomes.ecommerce.cart.application.RemoveCartItemCommand;
import me.ronygomes.ecommerce.cart.application.UpdateCartItemQtyCommand;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.infrastructure.RabbitMQCommandBus;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

public class CartCommandApi {
    static void main() {
        Javalin app = Javalin.create(config -> {
        }).start(8083);

        CommandBus commandBus = new RabbitMQCommandBus("cart_commands", "localhost");
        ObjectMapper objectMapper = new ObjectMapper();

        app.post("/cart/{guestToken}/items", ctx -> {
            String guestToken = ctx.pathParam("guestToken");
            AddItemToCartRequest body = objectMapper.readValue(ctx.body(), AddItemToCartRequest.class);
            commandBus.send(new AddCartItemCommand(guestToken, UUID.fromString(body.productId()), body.qty()));
            ctx.status(202);
            ctx.result("{}");
        });

        app.put("/cart/{guestToken}/items/{productId}", ctx -> {
            String guestToken = ctx.pathParam("guestToken");
            String productId = ctx.pathParam("productId");
            UpdateCartItemRequest body = objectMapper.readValue(ctx.body(), UpdateCartItemRequest.class);
            commandBus.send(new UpdateCartItemQtyCommand(guestToken, UUID.fromString(productId), body.qty()));
            ctx.status(202);
            ctx.result("{}");
        });

        app.delete("/cart/{guestToken}/items/{productId}", ctx -> {
            String guestToken = ctx.pathParam("guestToken");
            String productId = ctx.pathParam("productId");
            commandBus.send(new RemoveCartItemCommand(guestToken, UUID.fromString(productId)));
            ctx.status(202);
            ctx.result("{}");
        });

        app.delete("/cart/{guestToken}", ctx -> {
            String guestToken = ctx.pathParam("guestToken");
            commandBus.send(new ClearCartCommand(guestToken));
            ctx.status(202);
            ctx.result("{}");
        });
    }

    public record AddItemToCartRequest(String productId, int qty) {
    }

    public record UpdateCartItemRequest(int qty) {
    }
}
