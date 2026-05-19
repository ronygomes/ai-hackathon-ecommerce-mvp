package me.ronygomes.ecommerce.cart.presentation.commandapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.HttpStatus;
import me.ronygomes.ecommerce.cart.application.AddCartItemCommand;
import me.ronygomes.ecommerce.cart.application.RemoveCartItemCommand;
import me.ronygomes.ecommerce.cart.application.UpdateCartItemQtyCommand;
import me.ronygomes.ecommerce.checkout.saga.message.command.ClearCartCommand;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.infrastructure.AppConfig;
import me.ronygomes.ecommerce.core.infrastructure.RabbitMQCommandBus;
import me.ronygomes.ecommerce.core.infrastructure.Validator;
import me.ronygomes.ecommerce.core.infrastructure.WebHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class CartCommandApi {

    private static final Logger log = LoggerFactory.getLogger(CartCommandApi.class);

    public static void register(JavalinConfig config, CommandBus commandBus, ObjectMapper objectMapper,
                                Validator validator) {

        config.routes.post("/cart/{guestToken}/items", ctx -> {
            String guestToken = ctx.pathParam("guestToken");
            AddItemToCartRequest body = objectMapper.readValue(ctx.body(), AddItemToCartRequest.class);
            AddCartItemCommand command = new AddCartItemCommand(
                    guestToken, UUID.fromString(body.productId()), body.qty());
            validator.validate(command);
            commandBus.send(command);
            ctx.status(HttpStatus.ACCEPTED);
            ctx.result("{}");
        });

        config.routes.put("/cart/{guestToken}/items/{productId}", ctx -> {
            String guestToken = ctx.pathParam("guestToken");
            String productId = ctx.pathParam("productId");
            UpdateCartItemRequest body = objectMapper.readValue(ctx.body(), UpdateCartItemRequest.class);
            UpdateCartItemQtyCommand command = new UpdateCartItemQtyCommand(
                    guestToken, UUID.fromString(productId), body.qty());
            validator.validate(command);
            commandBus.send(command);
            ctx.status(HttpStatus.ACCEPTED);
            ctx.result("{}");
        });

        config.routes.delete("/cart/{guestToken}/items/{productId}", ctx -> {
            String guestToken = ctx.pathParam("guestToken");
            String productId = ctx.pathParam("productId");
            RemoveCartItemCommand command = new RemoveCartItemCommand(guestToken, UUID.fromString(productId));
            validator.validate(command);
            commandBus.send(command);
            ctx.status(HttpStatus.ACCEPTED);
            ctx.result("{}");
        });

        config.routes.delete("/cart/{guestToken}", ctx -> {
            String guestToken = ctx.pathParam("guestToken");
            // User-initiated clears aren't part of a saga; the fresh correlationId flows
            // to the event harmlessly and the saga's CartCleared handler will ignore it
            // (no matching saga state).
            // User-initiated, no upstream event — causationId is null (originating message).
            commandBus.send(new ClearCartCommand(guestToken, UUID.randomUUID(), null));
            ctx.status(HttpStatus.ACCEPTED);
            ctx.result("{}");
        });

        WebHelper.registerDefaultExceptionHandler(config, log);
    }

    static void main() {
        AppConfig appConfig = AppConfig.fromEnv();
        CommandBus commandBus = new RabbitMQCommandBus("cart_commands", appConfig.rabbitHost());
        ObjectMapper objectMapper = new ObjectMapper();
        Validator validator = new Validator();

        Javalin.create(cfg -> register(cfg, commandBus, objectMapper, validator)).start(8084);
    }

    public record AddItemToCartRequest(String productId, int qty) {
    }

    public record UpdateCartItemRequest(int qty) {
    }
}
