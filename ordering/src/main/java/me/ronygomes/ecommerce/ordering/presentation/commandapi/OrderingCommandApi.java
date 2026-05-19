package me.ronygomes.ecommerce.ordering.presentation.commandapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.HttpStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.infrastructure.AppConfig;
import me.ronygomes.ecommerce.core.infrastructure.RabbitMQCommandBus;
import me.ronygomes.ecommerce.core.infrastructure.Validator;
import me.ronygomes.ecommerce.core.infrastructure.WebHelper;
import me.ronygomes.ecommerce.ordering.application.PlaceOrderCommand;
import me.ronygomes.ecommerce.ordering.domain.CustomerInfo;
import me.ronygomes.ecommerce.ordering.domain.ShippingAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

public class OrderingCommandApi {

    private static final Logger log = LoggerFactory.getLogger(OrderingCommandApi.class);

    public static void register(JavalinConfig config, CommandBus commandBus, ObjectMapper objectMapper,
                                Validator validator) {

        config.routes.post("/orders", ctx -> {
            PlaceOrderRequest body = objectMapper.readValue(ctx.body(), PlaceOrderRequest.class);
            // Validate request DTO first — idempotencyKey is required for orderId derivation below.
            validator.validate(body);
            UUID orderId = UUID.nameUUIDFromBytes(body.idempotencyKey().getBytes());
            PlaceOrderCommand command = new PlaceOrderCommand(
                    orderId,
                    body.guestToken(),
                    body.cartId(),
                    body.customerInfo(),
                    body.address(),
                    body.idempotencyKey(),
                    body.items());

            commandBus.send(command);

            ctx.status(HttpStatus.ACCEPTED);
            ctx.contentType("application/json");
            ctx.result("{\"orderId\": \"" + orderId + "\", \"status\": \"Accepted\"}");
        });

        WebHelper.registerDefaultExceptionHandler(config, log);
    }

    public record PlaceOrderRequest(
            @NotBlank(message = "guestToken cannot be empty") String guestToken,
            @NotBlank(message = "cartId cannot be empty") String cartId,
            @NotNull CustomerInfo customerInfo,
            @NotNull(message = "address cannot be null") ShippingAddress address,
            @NotBlank(message = "idempotencyKey cannot be empty") String idempotencyKey,
            @NotEmpty(message = "items cannot be empty") List<PlaceOrderCommand.OrderItemRequest> items) {
    }

    static void main() {
        AppConfig appConfig = AppConfig.fromEnv();
        CommandBus commandBus = new RabbitMQCommandBus("ordering_commands", appConfig.rabbitHost());
        ObjectMapper objectMapper = new ObjectMapper();
        Validator validator = new Validator();

        Javalin.create(cfg -> register(cfg, commandBus, objectMapper, validator)).start(8086);
    }
}
