package me.ronygomes.ecommerce.ordering.application;

import me.rongyomes.ecommerce.checkout.saga.message.event.CheckoutRequested;
import me.ronygomes.ecommerce.core.domain.DomainEvent;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
import me.ronygomes.ecommerce.ordering.domain.CustomerInfo;
import me.ronygomes.ecommerce.ordering.domain.IdempotencyKey;
import me.ronygomes.ecommerce.ordering.domain.Order;
import me.ronygomes.ecommerce.ordering.domain.OrderId;
import me.ronygomes.ecommerce.ordering.domain.ShippingAddress;
import me.ronygomes.ecommerce.ordering.infrastructure.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlaceOrderHandlerTest {

    private final OrderRepository repository = mock(OrderRepository.class);
    private final MessageBus messageBus = mock(MessageBus.class);
    private PlaceOrderHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PlaceOrderHandler(repository, messageBus);
        when(repository.save(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    private static PlaceOrderCommand sampleCommand(UUID idempotencyKey) {
        return new PlaceOrderCommand(
                "g1",
                "g1",
                new CustomerInfo("Jane", "+1", "jane@example.com"),
                new ShippingAddress("1 Main", "Anytown", "12345", "USA"),
                idempotencyKey.toString(),
                List.of(new PlaceOrderCommand.OrderItemRequest(
                        UUID.randomUUID(), "SKU-1", "Widget", 10.0, 2)));
    }

    @Test
    void handle_newKey_savesAndPublishesCheckoutRequestedAndReturnsOrderId() throws Exception {
        when(repository.getByIdempotencyKey(any())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        AtomicReference<List<DomainEvent>> published = new AtomicReference<>();
        doAnswer(inv -> {
            published.set(new ArrayList<>(inv.getArgument(0)));
            return CompletableFuture.completedFuture(null);
        }).when(messageBus).publish(any());

        UUID result = handler.handle(sampleCommand(UUID.randomUUID())).get();

        assertThat(result).isNotNull();
        verify(repository).save(any(Order.class));
        assertThat(published.get()).singleElement().isInstanceOf(CheckoutRequested.class);
    }

    @Test
    void handle_existingKey_returnsExistingOrderIdAndDoesNotSave() throws Exception {
        UUID key = UUID.randomUUID();
        Order existing = Order.place(
                OrderId.generate(),
                new me.ronygomes.ecommerce.ordering.domain.GuestToken("g1"),
                new CustomerInfo("Jane", "+1", "j@e"),
                new ShippingAddress("L", "C", "x", "US"),
                List.of(new me.ronygomes.ecommerce.ordering.domain.OrderLineItem(
                        UUID.randomUUID(), "S", "N", 1.0, 1)),
                new IdempotencyKey(key));
        when(repository.getByIdempotencyKey(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(existing)));

        UUID result = handler.handle(sampleCommand(key)).get();

        assertThat(result).isEqualTo(existing.getId().value());
        verify(repository, never()).save(any());
        verify(messageBus, never()).publish(any());
    }

    @Test
    void handle_invalidIdempotencyKey_throwsSynchronously() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                handler.handle(sampleCommand_withInvalidKey()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private PlaceOrderCommand sampleCommand_withInvalidKey() {
        return new PlaceOrderCommand(
                "g1",
                "g1",
                new CustomerInfo("Jane", "+1", "j@e"),
                new ShippingAddress("L", "C", "x", "US"),
                "not-a-uuid",
                List.of(new PlaceOrderCommand.OrderItemRequest(UUID.randomUUID(), "S", "N", 1.0, 1)));
    }
}
