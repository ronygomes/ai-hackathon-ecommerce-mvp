package me.ronygomes.ecommerce.core.messaging;

import me.ronygomes.ecommerce.core.application.Command;
import me.ronygomes.ecommerce.core.application.CommandHandler;
import me.ronygomes.ecommerce.core.infrastructure.idempotency.ProcessedCommandStore;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommandHandlerDispatcherAdapterTest {

    private record SampleCommand(String payload) implements Command<UUID> {
    }

    @Test
    @SuppressWarnings("unchecked")
    void handle_delegatesToCommandHandlerAndDiscardsResult() throws Exception {
        CommandHandler<SampleCommand, UUID> handler = mock(CommandHandler.class);
        UUID result = UUID.randomUUID();
        SampleCommand command = new SampleCommand("x");
        when(handler.handle(eq(command))).thenReturn(CompletableFuture.completedFuture(result));

        CommandHandlerDispatcherAdapter<SampleCommand, UUID> adapter =
                new CommandHandlerDispatcherAdapter<>(handler, SampleCommand.class);

        CompletableFuture<Void> future = adapter.handle(command);

        verify(handler).handle(eq(command));
        assertThat(future.get()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void handle_propagatesHandlerFailure() {
        CommandHandler<SampleCommand, UUID> handler = mock(CommandHandler.class);
        RuntimeException boom = new RuntimeException("nope");
        when(handler.handle(org.mockito.ArgumentMatchers.any()))
                .thenReturn(CompletableFuture.failedFuture(boom));

        CommandHandlerDispatcherAdapter<SampleCommand, UUID> adapter =
                new CommandHandlerDispatcherAdapter<>(handler, SampleCommand.class);

        CompletableFuture<Void> future = adapter.handle(new SampleCommand("x"));

        assertThat(future).isCompletedExceptionally();
        assertThatThrownBy(future::get).hasCause(boom);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getMessageType_returnsConfiguredClass() {
        CommandHandlerDispatcherAdapter<SampleCommand, UUID> adapter =
                new CommandHandlerDispatcherAdapter<>(mock(CommandHandler.class), SampleCommand.class);

        assertThat(adapter.getMessageType()).isEqualTo(SampleCommand.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleWithMetadata_withoutStore_delegatesToCommandHandlerIgnoringMetadata() throws Exception {
        // 2-arg constructor → no ProcessedCommandStore → metadata variant delegates to no-metadata handle.
        CommandHandler<SampleCommand, UUID> handler = mock(CommandHandler.class);
        SampleCommand command = new SampleCommand("x");
        when(handler.handle(eq(command))).thenReturn(CompletableFuture.completedFuture(UUID.randomUUID()));

        CommandHandlerDispatcherAdapter<SampleCommand, UUID> adapter =
                new CommandHandlerDispatcherAdapter<>(handler, SampleCommand.class);

        CompletableFuture<Void> future = adapter.handle(command, MessageMetadata.withCommandId("cmd-1"));

        verify(handler).handle(eq(command));
        assertThat(future.get()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleWithMetadata_firstSeenCommand_callsHandlerAndMarksProcessed() throws Exception {
        CommandHandler<SampleCommand, UUID> handler = mock(CommandHandler.class);
        ProcessedCommandStore store = mock(ProcessedCommandStore.class);
        SampleCommand command = new SampleCommand("x");
        when(handler.handle(eq(command))).thenReturn(CompletableFuture.completedFuture(UUID.randomUUID()));
        when(store.wasProcessed("cmd-1")).thenReturn(false);

        CommandHandlerDispatcherAdapter<SampleCommand, UUID> adapter =
                new CommandHandlerDispatcherAdapter<>(handler, SampleCommand.class, store);

        adapter.handle(command, MessageMetadata.withCommandId("cmd-1")).get();

        verify(handler).handle(eq(command));
        verify(store).markProcessed(eq("cmd-1"), eq("SampleCommand"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleWithMetadata_duplicateCommand_skipsHandlerAndDoesNotMarkAgain() throws Exception {
        CommandHandler<SampleCommand, UUID> handler = mock(CommandHandler.class);
        ProcessedCommandStore store = mock(ProcessedCommandStore.class);
        when(store.wasProcessed("cmd-1")).thenReturn(true);

        CommandHandlerDispatcherAdapter<SampleCommand, UUID> adapter =
                new CommandHandlerDispatcherAdapter<>(handler, SampleCommand.class, store);

        adapter.handle(new SampleCommand("x"), MessageMetadata.withCommandId("cmd-1")).get();

        verify(handler, never()).handle(any());
        verify(store, never()).markProcessed(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleWithMetadata_handlerFails_doesNotMarkProcessed() {
        CommandHandler<SampleCommand, UUID> handler = mock(CommandHandler.class);
        ProcessedCommandStore store = mock(ProcessedCommandStore.class);
        when(store.wasProcessed("cmd-1")).thenReturn(false);
        RuntimeException boom = new RuntimeException("kaboom");
        when(handler.handle(any())).thenReturn(CompletableFuture.failedFuture(boom));

        CommandHandlerDispatcherAdapter<SampleCommand, UUID> adapter =
                new CommandHandlerDispatcherAdapter<>(handler, SampleCommand.class, store);

        CompletableFuture<Void> future = adapter.handle(
                new SampleCommand("x"), MessageMetadata.withCommandId("cmd-1"));

        assertThat(future).isCompletedExceptionally();
        verify(store, never()).markProcessed(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleWithMetadata_nullCommandId_skipsIdempotencyAndCallsHandler() throws Exception {
        CommandHandler<SampleCommand, UUID> handler = mock(CommandHandler.class);
        ProcessedCommandStore store = mock(ProcessedCommandStore.class);
        SampleCommand command = new SampleCommand("x");
        when(handler.handle(eq(command))).thenReturn(CompletableFuture.completedFuture(UUID.randomUUID()));

        CommandHandlerDispatcherAdapter<SampleCommand, UUID> adapter =
                new CommandHandlerDispatcherAdapter<>(handler, SampleCommand.class, store);

        adapter.handle(command, MessageMetadata.empty()).get();

        verify(handler).handle(eq(command));
        verify(store, never()).wasProcessed(any());
        verify(store, never()).markProcessed(any(), any());
    }
}
