package me.ronygomes.ecommerce.inventory.application;

import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.inventory.domain.InventoryItem;
import me.ronygomes.ecommerce.inventory.domain.ProductId;
import me.ronygomes.ecommerce.inventory.domain.Quantity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ValidateStockHandlerTest {

    @SuppressWarnings("unchecked")
    private final Repository<InventoryItem, ProductId> repository = mock(Repository.class);
    private ValidateStockHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ValidateStockHandler(repository);
    }

    private static InventoryItem stocked(int qty) {
        return InventoryItem.create(ProductId.generate(), new Quantity(qty));
    }

    @Test
    void handle_allItemsAvailable_returnsTrue() throws Exception {
        when(repository.getById(any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(stocked(10))))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(stocked(20))));

        Boolean result = handler.handle(new ValidateStockCommand(List.of(
                new ValidateStockCommand.StockItemRequest(UUID.randomUUID(), 5),
                new ValidateStockCommand.StockItemRequest(UUID.randomUUID(), 15)))).get();

        assertThat(result).isTrue();
    }

    @Test
    void handle_someItemUnderstocked_returnsFalse() throws Exception {
        when(repository.getById(any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(stocked(2))))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(stocked(20))));

        Boolean result = handler.handle(new ValidateStockCommand(List.of(
                new ValidateStockCommand.StockItemRequest(UUID.randomUUID(), 5),
                new ValidateStockCommand.StockItemRequest(UUID.randomUUID(), 15)))).get();

        assertThat(result).isFalse();
    }

    @Test
    void handle_itemNotFound_returnsFalse() throws Exception {
        when(repository.getById(any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        Boolean result = handler.handle(new ValidateStockCommand(List.of(
                new ValidateStockCommand.StockItemRequest(UUID.randomUUID(), 1)))).get();

        assertThat(result).isFalse();
    }

    @Test
    void handle_emptyRequest_returnsTrue() throws Exception {
        Boolean result = handler.handle(new ValidateStockCommand(List.of())).get();

        assertThat(result).isTrue();
    }
}
