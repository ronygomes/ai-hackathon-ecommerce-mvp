package me.ronygomes.ecommerce.productcatalog.presentation.commandapi;

import com.google.inject.Guice;
import io.javalin.Javalin;
import jakarta.inject.Inject;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.productcatalog.application.CreateProductCommand;
import me.ronygomes.ecommerce.productcatalog.infrastructure.CommandApiModule;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static me.ronygomes.ecommerce.core.infrastructure.WebHelper.registerDefaultExceptionHandler;

public class CommandApi {

    private static final Logger log = LoggerFactory.getLogger(CommandApi.class);
    public static final int PORT = 8080;

    private final CommandBus commandBus;

    @Inject
    public CommandApi(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    private void run(Javalin app) {

        app.post("/products", ctx -> {
            CreateProductCommand command = ctx.bodyAsClass(CreateProductCommand.class);
            log.trace("Received command: {}", command);
            commandBus.send(command);
            ctx.status(HttpStatus.ACCEPTED_202);
        });

        registerDefaultExceptionHandler(app, log);
    }

    static void main() {
        Javalin app = Javalin.create().start(PORT);

        Guice.createInjector(new CommandApiModule())
                .getInstance(CommandApi.class)
                .run(app);
    }
}
