package com.ecommerce.ordering.application;

import com.ecommerce.core.application.ICommandHandler;
import com.ecommerce.ordering.domain.*;
import com.ecommerce.ordering.infrastructure.IOrderRepository;
import com.ecommerce.checkout.saga.messages.commands.CreateOrderCommand;
import com.google.inject.Inject;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CreateOrderHandler implements ICommandHandler<CreateOrderCommand, Void> {
        private final IOrderRepository repository;

        @Inject
        public CreateOrderHandler(IOrderRepository repository) {
                this.repository = repository;
        }

        @Override
        public CompletableFuture<Void> handle(CreateOrderCommand command) {
                CustomerInfo customerInfo = new CustomerInfo(command.customerName(), command.customerPhone(),
                                command.customerEmail());
                ShippingAddress address = new ShippingAddress(command.addressLine1(), command.addressCity(),
                                command.addressZip(), command.addressCountry());

                List<OrderLineItem> items = command.items().stream()
                                .map(i -> new OrderLineItem(i.productId(), i.sku(), i.name(), i.unitPrice(), i.qty()))
                                .collect(Collectors.toList());

                Order order = Order.place(
                                OrderId.fromString(command.orderId().toString()),
                                new GuestToken(command.guestToken()),
                                customerInfo,
                                address,
                                items,
                                IdempotencyKey.fromString(command.idempotencyKey()));

                return repository.save(order);
        }
}
