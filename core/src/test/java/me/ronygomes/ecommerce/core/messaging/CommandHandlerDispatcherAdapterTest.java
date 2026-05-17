package me.ronygomes.ecommerce.core.messaging;

import me.ronygomes.ecommerce.core.application.Command;
import me.ronygomes.ecommerce.core.application.CommandHandler;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
    void handleWithMetadata_delegatesToCommandHandlerIgnoringMetadataForNow() throws Exception {
        // 4b.5 plumbs MessageMetadata through MessageDispatcher → MessageHandler. The adapter
        // doesn't read it yet (idempotency wiring is chunks 4c–4z); this test pins that the default
        // delegation from MessageHandler bridges metadata-variant calls back to the no-metadata handle.
        CommandHandler<SampleCommand, UUID> handler = mock(CommandHandler.class);
        SampleCommand command = new SampleCommand("x");
        when(handler.handle(eq(command))).thenReturn(CompletableFuture.completedFuture(UUID.randomUUID()));

        CommandHandlerDispatcherAdapter<SampleCommand, UUID> adapter =
                new CommandHandlerDispatcherAdapter<>(handler, SampleCommand.class);

        CompletableFuture<Void> future = adapter.handle(command, MessageMetadata.withCommandId("cmd-1"));

        verify(handler).handle(eq(command));
        assertThat(future.get()).isNull();
    }
}
