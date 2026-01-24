package com.ecommerce.cart.application;

import com.ecommerce.core.application.ICommand;
import java.util.UUID;

public record AddCartItemCommand(String guestToken, UUID productId, int qty) implements ICommand<Void> {
}
