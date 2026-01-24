package com.ecommerce.checkout.saga.messages.commands;

import com.ecommerce.core.application.ICommand;

public record ClearCartCommand(String guestToken) implements ICommand<Void> {
}
