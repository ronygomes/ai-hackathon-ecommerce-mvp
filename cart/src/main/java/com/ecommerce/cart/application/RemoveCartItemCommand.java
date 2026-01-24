package com.ecommerce.cart.application;

import com.ecommerce.core.application.ICommand;
import java.util.UUID;

public record RemoveCartItemCommand(String guestToken, UUID productId) implements ICommand<Void> {
}
