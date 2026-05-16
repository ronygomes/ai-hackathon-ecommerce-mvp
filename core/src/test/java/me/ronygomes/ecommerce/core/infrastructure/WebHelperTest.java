package me.ronygomes.ecommerce.core.infrastructure;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class WebHelperTest {

    private Javalin appThatThrows(Exception thrown) {
        return Javalin.create(config -> {
            config.routes.get("/boom", ctx -> {
                throw thrown;
            });
            WebHelper.registerDefaultExceptionHandler(config, LoggerFactory.getLogger(WebHelperTest.class));
        });
    }

    @Test
    void validationException_returns400WithErrorBody() {
        JavalinTest.test(appThatThrows(new ValidationException("name required")), (server, client) -> {
            var response = client.get("/boom");
            assertThat(response.code()).isEqualTo(HttpStatus.BAD_REQUEST.getCode());
            assertThat(response.body().string()).contains("name required");
        });
    }

    @Test
    void invalidFormatException_returns400WithValueAndType() {
        InvalidFormatException ife = new InvalidFormatException("bad number", "abc", Double.class);
        JavalinTest.test(appThatThrows(ife), (server, client) -> {
            var response = client.get("/boom");
            assertThat(response.code()).isEqualTo(HttpStatus.BAD_REQUEST.getCode());
            assertThat(response.body().string()).contains("'abc' is not a valid java.lang.Double type");
        });
    }

    @Test
    void jsonParseException_returns400WithInvalidJsonMessage() {
        JsonParseException jpe = new JsonParseException(null, "bad json");
        JavalinTest.test(appThatThrows(jpe), (server, client) -> {
            var response = client.get("/boom");
            assertThat(response.code()).isEqualTo(HttpStatus.BAD_REQUEST.getCode());
            assertThat(response.body().string()).isEqualTo("Invalid JSON Request");
        });
    }

    @Test
    void uncaughtException_returns500WithDefaultErrorMessage() {
        JavalinTest.test(appThatThrows(new RuntimeException("kaboom")), (server, client) -> {
            var response = client.get("/boom");
            assertThat(response.code()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
            assertThat(response.body().string()).contains("The application has encountered an unknown error");
        });
    }
}
