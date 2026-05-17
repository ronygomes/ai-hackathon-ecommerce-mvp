package me.ronygomes.ecommerce.core.messaging;

public record MessageMetadata(String commandId) {

    public static MessageMetadata empty() {
        return new MessageMetadata(null);
    }

    public static MessageMetadata withCommandId(String commandId) {
        return new MessageMetadata(commandId);
    }
}
