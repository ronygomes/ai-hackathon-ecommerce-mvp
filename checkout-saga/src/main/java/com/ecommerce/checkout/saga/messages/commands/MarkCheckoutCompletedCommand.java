package com.ecommerce.checkout.saga.messages.commands;

import com.ecommerce.core.application.ICommand;
import java.util.UUID;

public record MarkCheckoutCompletedCommand(UUID orderId) implements ICommand<Void> {
}
