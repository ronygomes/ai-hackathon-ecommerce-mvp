package me.ronygomes.ecommerce.core.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageDispatcherImplTest {

    private record Payload(String value) {
    }

    private MessageDispatcherImpl dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new MessageDispatcherImpl(new ObjectMapper());
    }

    @Test
    void dispatch_withRegisteredHandler_deserializesAndInvokesHandler() throws Exception {
        AtomicReference<Payload> received = new AtomicReference<>();
        dispatcher.registerHandler("Payload", new MessageHandler<Payload>() {
            @Override
            public CompletableFuture<Void> handle(Payload message) {
                received.set(message);
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public Class<Payload> getMessageType() {
                return Payload.class;
            }
        });

        dispatcher.dispatch("Payload", "{\"value\":\"hello\"}").get();

        assertThat(received.get()).isEqualTo(new Payload("hello"));
    }

    @Test
    void dispatch_withoutRegisteredHandler_returnsCompletedFutureWithoutThrowing() throws Exception {
        CompletableFuture<Void> result = dispatcher.dispatch("Unknown", "{}");

        assertThat(result).isCompleted();
        assertThat(result.get()).isNull();
    }

    @Test
    void dispatch_withMalformedJson_returnsExceptionallyCompletedFuture() {
        dispatcher.registerHandler("Payload", new MessageHandler<Payload>() {
            @Override
            public CompletableFuture<Void> handle(Payload message) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public Class<Payload> getMessageType() {
                return Payload.class;
            }
        });

        CompletableFuture<Void> result = dispatcher.dispatch("Payload", "{ not json");

        assertThat(result).isCompletedExceptionally();
        assertThatThrownBy(result::get).isInstanceOf(ExecutionException.class);
    }

    @Test
    void dispatch_propagatesFuturefromHandler() {
        RuntimeException boom = new RuntimeException("handler boom");
        dispatcher.registerHandler("Payload", new MessageHandler<Payload>() {
            @Override
            public CompletableFuture<Void> handle(Payload message) {
                return CompletableFuture.failedFuture(boom);
            }

            @Override
            public Class<Payload> getMessageType() {
                return Payload.class;
            }
        });

        CompletableFuture<Void> result = dispatcher.dispatch("Payload", "{\"value\":\"x\"}");

        assertThat(result).isCompletedExceptionally();
        assertThatThrownBy(result::get)
                .isInstanceOf(ExecutionException.class)
                .hasCause(boom);
    }
}
