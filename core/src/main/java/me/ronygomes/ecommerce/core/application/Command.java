package me.ronygomes.ecommerce.core.application;

public interface Command<TResponse> {

    String HEADER_MESSAGE_TYPE = "X-Message-Type";
    String HEADER_COMMAND_ID = "X-Command-Id";
}
