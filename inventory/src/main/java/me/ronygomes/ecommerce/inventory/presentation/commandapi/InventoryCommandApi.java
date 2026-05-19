package me.ronygomes.ecommerce.inventory.presentation.commandapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.HttpStatus;
import me.ronygomes.ecommerce.checkout.saga.message.command.DeductStockForOrderCommand;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.infrastructure.AppConfig;
import me.ronygomes.ecommerce.core.infrastructure.RabbitMQCommandBus;
import me.ronygomes.ecommerce.core.infrastructure.Validator;
import me.ronygomes.ecommerce.core.infrastructure.WebHelper;
import me.ronygomes.ecommerce.inventory.application.SetStockCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InventoryCommandApi {

    private static final Logger log = LoggerFactory.getLogger(InventoryCommandApi.class);

    public static void register(JavalinConfig config, CommandBus commandBus, ObjectMapper objectMapper,
                                Validator validator) {

        config.routes.post("/inventory/stock", ctx -> {
            SetStockCommand command = objectMapper.readValue(ctx.body(), SetStockCommand.class);
            validator.validate(command);
            commandBus.send(command);
            ctx.status(HttpStatus.ACCEPTED);
            ctx.result("{\"status\": \"Accepted\"}");
        });

        config.routes.post("/inventory/deductions", ctx -> {
            DeductStockForOrderCommand command = objectMapper.readValue(ctx.body(), DeductStockForOrderCommand.class);
            validator.validate(command);
            commandBus.send(command);
            ctx.status(HttpStatus.ACCEPTED);
            ctx.result("{\"status\": \"Accepted\"}");
        });

        WebHelper.registerDefaultExceptionHandler(config, log);
    }

    static void main() {
        AppConfig config = AppConfig.fromEnv();
        CommandBus commandBus = new RabbitMQCommandBus("inventory_commands", config.rabbitHost());
        ObjectMapper objectMapper = new ObjectMapper();
        Validator validator = new Validator();

        Javalin.create(cfg -> register(cfg, commandBus, objectMapper, validator)).start(8082);
    }
}
