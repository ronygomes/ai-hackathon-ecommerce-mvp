package me.ronygomes.ecommerce.productcatalog.presentation.commandapi;

import com.google.inject.Guice;
import io.javalin.Javalin;
import jakarta.inject.Inject;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.productcatalog.application.CreateProductCommand;
import me.ronygomes.ecommerce.productcatalog.infrastructure.CommandApiModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandApi {

    private static final Logger log = LoggerFactory.getLogger(CommandApi.class);
    public static final int PORT = 8080;

    private final CommandBus commandBus;

    @Inject
    public CommandApi(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    private void run() {
        Javalin app = Javalin.create().start(PORT);

        app.post("/products", ctx -> {
            CreateProductCommand command = ctx.bodyAsClass(CreateProductCommand.class);
            log.info("Received command: {}", command);
            commandBus.send(command);
            ctx.status(202);
            ctx.result("{\"status\": \"Accepted\"}");
        });

        app.exception(Exception.class, (e, ctx) -> {
            log.error("Error processing command", e);
            ctx.status(500);
            ctx.result("{\"error\": \"" + e.getMessage() + "\"}");
        });
    }

    static void main() {
        Guice.createInjector(new CommandApiModule())
                .getInstance(CommandApi.class)
                .run();
    }
}
