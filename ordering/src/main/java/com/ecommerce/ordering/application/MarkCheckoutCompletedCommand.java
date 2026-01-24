package com.ecommerce.ordering.application;

import com.ecommerce.core.application.ICommand;
import java.util.UUID;

public record MarkCheckoutCompletedCommand(UUID orderId) implements ICommand<Void> {
}
