package me.ronygomes.ecommerce.core.infrastructure.outbox;

import me.ronygomes.ecommerce.core.messaging.MessageBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OutboxDispatcherTest {

    private final OutboxStore store = mock(OutboxStore.class);
    private final MessageBus messageBus = mock(MessageBus.class);
    private OutboxDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new OutboxDispatcher(store, messageBus, 10);
        when(messageBus.publishRaw(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void tick_publishesEveryPendingEntryAndMarksThemPublished() {
        OutboxEntry a = new OutboxEntry("id-a", "agg-1", "EventA", "{\"x\":1}", 1L, null);
        OutboxEntry b = new OutboxEntry("id-b", "agg-1", "EventB", "{\"y\":2}", 2L, null);
        when(store.findPending(10)).thenReturn(List.of(a, b));

        int processed = dispatcher.tick();

        assertThat(processed).isEqualTo(2);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MessageBus.RawMessage>> msgs = ArgumentCaptor.forClass(List.class);
        verify(messageBus).publishRaw(msgs.capture());
        assertThat(msgs.getValue())
                .extracting(MessageBus.RawMessage::type)
                .containsExactly("EventA", "EventB");
        assertThat(msgs.getValue())
                .extracting(m -> new String(m.payload()))
                .containsExactly("{\"x\":1}", "{\"y\":2}");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> idsCaptor = ArgumentCaptor.forClass(List.class);
        verify(store).markPublished(idsCaptor.capture());
        assertThat(idsCaptor.getValue()).containsExactly("id-a", "id-b");
    }

    @Test
    void tick_emptyPending_doesNothingAndReturnsZero() {
        when(store.findPending(10)).thenReturn(List.of());

        int processed = dispatcher.tick();

        assertThat(processed).isZero();
        verify(messageBus, never()).publishRaw(any());
        verify(store, never()).markPublished(any());
    }

    @Test
    void tick_whenPublishFails_propagatesAndDoesNotMarkPublished() {
        OutboxEntry a = new OutboxEntry("id-a", "agg-1", "EventA", "{}", 1L, null);
        when(store.findPending(10)).thenReturn(List.of(a));
        when(messageBus.publishRaw(any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("rabbit down")));

        assertThatThrownBy(() -> dispatcher.tick())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Outbox dispatch failed");

        verify(store, never()).markPublished(any());
    }

    @Test
    void constructor_zeroOrNegativeBatchSize_throws() {
        assertThatThrownBy(() -> new OutboxDispatcher(store, messageBus, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OutboxDispatcher(store, messageBus, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
