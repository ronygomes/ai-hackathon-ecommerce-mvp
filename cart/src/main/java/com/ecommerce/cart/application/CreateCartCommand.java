package com.ecommerce.cart.application;

import com.ecommerce.core.application.ICommand;

public record CreateCartCommand(String guestToken) implements ICommand<Void> {
}
