package me.ronygomes.ecommerce.core.infrastructure;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import io.javalin.Javalin;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;

public class WebHelper {

    private static final String ERROR_JSON_TEMPLATE = """
            {
                "error": %s
            }""";

    private static final String INVALID_FORMAT_ERROR_MESSAGE = "'%s' is not a valid %s type";
    private static final String DEFAULT_ERROR_MESSAGE = "The application has encountered an unknown error";

    public static void registerDefaultExceptionHandler(Javalin app, Logger log) {
        app.exception(InvalidFormatException.class, (e, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST_400);
            ctx.result(ERROR_JSON_TEMPLATE.formatted(
                    INVALID_FORMAT_ERROR_MESSAGE.formatted(e.getValue(), e.getTargetType().getName())
            ));
        });

        app.exception(Exception.class, (e, ctx) -> {
            log.error("Error processing command", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
            ctx.result(ERROR_JSON_TEMPLATE.formatted(DEFAULT_ERROR_MESSAGE));
        });
    }
}
