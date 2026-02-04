package me.ronygomes.ecommerce.productcatalog.presentation.commandapi;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.infrastructure.Validator;
import me.ronygomes.ecommerce.productcatalog.application.CreateProductCommand;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CommandApiTest {

    @Mock
    private CommandBus commandBus;

    @Spy
    private Validator validator;

    private CommandApi commandApi;

    @BeforeEach
    void setUp() {
        commandApi = new CommandApi(commandBus, validator);
    }

    @Test
    void postProducts_withValidData_shouldReturn202() {
        String body = """
                {
                    "sku": "SKU-123",
                    "name": "Product Name",
                    "price": 99.99,
                    "description": "Product Description"
                }
                """;

        when(commandBus.send(any(CreateProductCommand.class))).thenReturn(CompletableFuture.completedFuture(null));

        JavalinTest.test(setupApp(), (server, client) -> {
            var response = client.post("/products", body);
            assertThat(response.code()).isEqualTo(HttpStatus.ACCEPTED_202);

            var commandCaptor = ArgumentCaptor.forClass(CreateProductCommand.class);
            verify(validator).validate(commandCaptor.capture());

            CreateProductCommand capturedCommand = commandCaptor.getValue();
            assertThat(capturedCommand.sku()).isEqualTo("SKU-123");
            assertThat(capturedCommand.name()).isEqualTo("Product Name");
            assertThat(capturedCommand.price()).isEqualTo(99.99);
            assertThat(capturedCommand.description()).isEqualTo("Product Description");

            verify(commandBus).send(Mockito.eq(capturedCommand));
        });
    }

    @Test
    void postProducts_withInvalidData_shouldReturn400() {
        String body = """
                {
                    "sku": "",
                    "name": "Product Name",
                    "price": 99.99,
                    "description": "Product Description"
                }
                """;

        JavalinTest.test(setupApp(), (server, client) -> {
            var response = client.post("/products", body);
            assertThat(response.code()).isEqualTo(HttpStatus.BAD_REQUEST_400);
            assertThat(response.body().string()).contains("sku cannot be empty");
        });
    }

    @Test
    void postProducts_withInvalidFormattedData_shouldReturn400() {
        String body = """
                {
                    "sku": "",
                    "name": "Product Name",
                    "price": "abc",
                    "description": "Product Description"
                }
                """;

        JavalinTest.test(setupApp(), (server, client) -> {
            var response = client.post("/products", body);
            assertThat(response.code()).isEqualTo(HttpStatus.BAD_REQUEST_400);
            assertThat(response.body().string()).contains("'abc' is not a valid double type");
        });
    }

    @Test
    void postProducts_withInvalidJson_shouldReturn400() {
        String body = """
                {
                    "sku": ""
                    "name": "Product Name",
                    "price": "abc",
                    "description": "Product Description"
                }
                """;

        JavalinTest.test(setupApp(), (server, client) -> {
            var response = client.post("/products", body);
            assertThat(response.code()).isEqualTo(HttpStatus.BAD_REQUEST_400);
            assertThat(response.body().string()).contains("Invalid JSON Request");
        });
    }

    @Test
    void postProducts_whenCommandBusThrowsException_shouldReturn500() {
        String body = """
                {
                    "sku": "SKU-123",
                    "name": "Product Name",
                    "price": 99.99,
                    "description": "Product Description"
                }
                """;

        when(commandBus.send(any())).thenThrow(new RuntimeException("Something went wrong"));

        JavalinTest.test(setupApp(), (server, client) -> {
            var response = client.post("/products", body);
            assertThat(response.code()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR_500);
            assertThat(response.body().string()).contains("The application has encountered an unknown error");
        });
    }

    private Javalin setupApp() {
        Javalin app = Javalin.create();
        commandApi.run(app);
        return app;
    }
}
