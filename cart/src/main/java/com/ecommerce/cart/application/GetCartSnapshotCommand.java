package com.ecommerce.cart.application;

import com.ecommerce.core.application.ICommand;

public record GetCartSnapshotCommand(String guestToken) implements ICommand<Void> {
}
