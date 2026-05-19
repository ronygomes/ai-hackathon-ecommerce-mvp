package me.ronygomes.ecommerce.core.messaging;

import me.ronygomes.ecommerce.core.domain.DomainEvent;
import me.ronygomes.ecommerce.core.infrastructure.idempotency.ProcessedCommandStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class IdempotentMessageHandlerTest {

    private record SampleEvent(String id) implements DomainEvent {
        @Override
        public String getEventId() {
            return id;
        }

        @Override
        public long getTimestamp() {
            return 0L;
        }
    }

    @SuppressWarnings("unchecked")
    private final MessageHandler<SampleEvent> delegate = mock(MessageHandler.class);
    private final ProcessedCommandStore store = mock(ProcessedCommandStore.class);
    private IdempotentMessageHandler<SampleEvent> handler;

    @BeforeEach
    void setUp() {
        handler = new IdempotentMessageHandler<>(delegate, store);
        when(delegate.getMessageType()).thenReturn(SampleEvent.class);
    }

    @Test
    void handle_firstSeenEventId_delegatesAndMarksProcessed() throws Exception {
        SampleEvent event = new SampleEvent("evt-1");
        when(store.wasProcessed("evt-1")).thenReturn(false);
        when(delegate.handle(eq(event), any())).thenReturn(CompletableFuture.completedFuture(null));

        handler.handle(event, MessageMetadata.empty()).get();

        verify(delegate).handle(eq(event), any());
        verify(store).markProcessed("evt-1", "SampleEvent");
    }

    @Test
    void handle_alreadyProcessedEventId_skipsDelegate() throws Exception {
        SampleEvent event = new SampleEvent("evt-1");
        when(store.wasProcessed("evt-1")).thenReturn(true);

        handler.handle(event, MessageMetadata.empty()).get();

        verify(delegate, never()).handle(any(), any());
        verify(store, never()).markProcessed(any(), any());
    }

    @Test
    void handle_nullEventId_bypassesIdempotencyEntirely() throws Exception {
        SampleEvent event = new SampleEvent(null);
        when(delegate.handle(eq(event), any())).thenReturn(CompletableFuture.completedFuture(null));

        handler.handle(event, MessageMetadata.empty()).get();

        verify(store, never()).wasProcessed(any());
        verify(store, never()).markProcessed(any(), any());
        verify(delegate).handle(eq(event), any());
    }

    @Test
    void handle_delegateFails_doesNotMarkProcessed() {
        SampleEvent event = new SampleEvent("evt-1");
        when(store.wasProcessed("evt-1")).thenReturn(false);
        CompletableFuture<Void> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("boom"));
        when(delegate.handle(eq(event), any())).thenReturn(failed);

        assertThatThrownBy(() -> handler.handle(event, MessageMetadata.empty()).get())
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("boom");

        verify(store, never()).markProcessed(any(), any());
    }

    @Test
    void getMessageType_returnsDelegateMessageType() {
        assertThat(handler.getMessageType()).isEqualTo(SampleEvent.class);
    }

    @Test
    void handle_singleArgOverload_routesThroughIdempotencyCheck() throws Exception {
        SampleEvent event = new SampleEvent("evt-2");
        when(store.wasProcessed("evt-2")).thenReturn(true);

        handler.handle(event).get();

        verify(delegate, never()).handle(any(), any());
    }
}
