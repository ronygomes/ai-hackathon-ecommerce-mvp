package me.ronygomes.ecommerce.productcatalog.presentation.commandapi;

import com.google.inject.Guice;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.HttpStatus;
import jakarta.inject.Inject;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.infrastructure.Validator;
import me.ronygomes.ecommerce.productcatalog.application.CreateProductCommand;
import me.ronygomes.ecommerce.productcatalog.infrastructure.CommandApiModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static me.ronygomes.ecommerce.core.infrastructure.WebHelper.registerDefaultExceptionHandler;

public class CommandApi {

    private static final Logger log = LoggerFactory.getLogger(CommandApi.class);
    public static final int PORT = 8080;

    private final CommandBus commandBus;
    private final Validator validator;

    @Inject
    public CommandApi(CommandBus commandBus, Validator validator) {
        this.commandBus = commandBus;
        this.validator = validator;
    }

    public void register(JavalinConfig config) {

        config.routes.post("/products", ctx -> {
            CreateProductRequest body = ctx.bodyAsClass(CreateProductRequest.class);
            UUID productId = UUID.randomUUID();
            CreateProductCommand command = new CreateProductCommand(
                    productId, body.sku(), body.name(), body.price(), body.description());
            log.trace("Received command: {}", command);

            validator.validate(command);
            commandBus.send(command);

            ctx.status(HttpStatus.ACCEPTED);
            ctx.contentType("application/json");
            ctx.result("{\"productId\":\"" + productId + "\"}");
        });

        registerDefaultExceptionHandler(config, log);
    }

    public record CreateProductRequest(String sku, String name, double price, String description) {
    }

    static void main() {
        CommandApi commandApi = Guice.createInjector(new CommandApiModule())
                .getInstance(CommandApi.class);

        Javalin.create(commandApi::register).start(PORT);
    }
}
