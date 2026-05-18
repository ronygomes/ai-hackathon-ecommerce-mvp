package me.ronygomes.ecommerce.core.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

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

    @AfterEach
    void cleanUpMdc() {
        MDC.clear();
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

        dispatcher.dispatch("Payload", "{\"value\":\"hello\"}", MessageMetadata.empty()).get();

        assertThat(received.get()).isEqualTo(new Payload("hello"));
    }

    @Test
    void dispatch_passesMetadataThroughToHandlerVariant() throws Exception {
        AtomicReference<MessageMetadata> receivedMetadata = new AtomicReference<>();
        dispatcher.registerHandler("Payload", new MessageHandler<Payload>() {
            @Override
            public CompletableFuture<Void> handle(Payload message) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Void> handle(Payload message, MessageMetadata metadata) {
                receivedMetadata.set(metadata);
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public Class<Payload> getMessageType() {
                return Payload.class;
            }
        });

        dispatcher.dispatch("Payload", "{\"value\":\"x\"}", MessageMetadata.withCommandId("cmd-7")).get();

        assertThat(receivedMetadata.get()).isNotNull();
        assertThat(receivedMetadata.get().commandId()).isEqualTo("cmd-7");
    }

    @Test
    void dispatch_withoutRegisteredHandler_returnsCompletedFutureWithoutThrowing() throws Exception {
        CompletableFuture<Void> result = dispatcher.dispatch("Unknown", "{}", MessageMetadata.empty());

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

        CompletableFuture<Void> result = dispatcher.dispatch("Payload", "{ not json", MessageMetadata.empty());

        assertThat(result).isCompletedExceptionally();
        assertThatThrownBy(result::get).isInstanceOf(ExecutionException.class);
    }

    @Test
    void dispatch_pushesCommandIdIntoMdcForHandlerInvocation_andRemovesAfter() throws Exception {
        AtomicReference<String> mdcDuringHandler = new AtomicReference<>();
        dispatcher.registerHandler("Payload", new MessageHandler<Payload>() {
            @Override
            public CompletableFuture<Void> handle(Payload message) {
                mdcDuringHandler.set(MDC.get("commandId"));
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public Class<Payload> getMessageType() {
                return Payload.class;
            }
        });

        dispatcher.dispatch("Payload", "{\"value\":\"x\"}", MessageMetadata.withCommandId("cmd-42")).get();

        assertThat(mdcDuringHandler.get()).isEqualTo("cmd-42");
        assertThat(MDC.get("commandId")).isNull();
    }

    @Test
    void dispatch_withNullCommandIdInMetadata_doesNotPushAnyMdcEntry() throws Exception {
        AtomicReference<String> mdcDuringHandler = new AtomicReference<>("not-set");
        dispatcher.registerHandler("Payload", new MessageHandler<Payload>() {
            @Override
            public CompletableFuture<Void> handle(Payload message) {
                mdcDuringHandler.set(MDC.get("commandId"));
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public Class<Payload> getMessageType() {
                return Payload.class;
            }
        });

        dispatcher.dispatch("Payload", "{\"value\":\"x\"}", MessageMetadata.empty()).get();

        assertThat(mdcDuringHandler.get()).isNull();
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

        CompletableFuture<Void> result = dispatcher.dispatch("Payload", "{\"value\":\"x\"}", MessageMetadata.empty());

        assertThat(result).isCompletedExceptionally();
        assertThatThrownBy(result::get)
                .isInstanceOf(ExecutionException.class)
                .hasCause(boom);
    }
}
