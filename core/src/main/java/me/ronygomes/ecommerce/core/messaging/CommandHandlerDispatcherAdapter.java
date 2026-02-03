package me.ronygomes.ecommerce.core.messaging;

import me.ronygomes.ecommerce.core.application.Command;
import me.ronygomes.ecommerce.core.application.CommandHandler;

import java.util.concurrent.CompletableFuture;

public class CommandHandlerDispatcherAdapter<TCommand extends Command<TResult>, TResult>
        implements MessageHandler<TCommand> {
    private final CommandHandler<TCommand, TResult> handler;
    private final Class<TCommand> commandType;

    public CommandHandlerDispatcherAdapter(CommandHandler<TCommand, TResult> handler, Class<TCommand> commandType) {
        this.handler = handler;
        this.commandType = commandType;
    }

    @Override
    public CompletableFuture<Void> handle(TCommand command) {
        return handler.handle(command).thenApply(r -> null);
    }

    @Override
    public Class<TCommand> getMessageType() {
        return commandType;
    }
}
