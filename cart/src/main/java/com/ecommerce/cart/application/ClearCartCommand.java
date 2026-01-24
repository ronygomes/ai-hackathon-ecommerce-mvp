package com.ecommerce.cart.application;

import com.ecommerce.core.application.ICommand;

public record ClearCartCommand(String guestToken) implements ICommand<Void> {
}
