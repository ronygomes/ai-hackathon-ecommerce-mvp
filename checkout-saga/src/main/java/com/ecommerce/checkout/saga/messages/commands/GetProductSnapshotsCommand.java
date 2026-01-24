package com.ecommerce.checkout.saga.messages.commands;

import com.ecommerce.core.application.ICommand;
import java.util.List;
import java.util.UUID;

public record GetProductSnapshotsCommand(List<UUID> productIds) implements ICommand<Void> {
}
