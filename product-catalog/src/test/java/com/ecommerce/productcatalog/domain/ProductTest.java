package com.ecommerce.productcatalog.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ProductTest {
    @Test
    public void createProduct_withValidData_shouldSucceed() {
        Product product = Product.create("Test Product", "SKU-123", 99.99, "Description");

        assertNotNull(product.getId());
        assertEquals("Test Product", product.getName());
        assertEquals("SKU-123", product.getSku());
        assertEquals(99.99, product.getPrice());
        assertEquals(1, product.getUncommittedEvents().size());
        assertTrue(product.getUncommittedEvents().get(0) instanceof ProductCreatedEvent);
    }

    @Test
    public void createProduct_withNegativePrice_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            Product.create("Test", "SKU", -1.0, "Desc");
        });
    }

    @Test
    public void updateProduct_withValidPrice_shouldSucceed() {
        Product product = Product.create("Test", "SKU", 10.0, "Desc");
        product.clearUncommittedEvents();

        product.updateDetails("New Name", 20.0, "New Desc");

        assertEquals("New Name", product.getName());
        assertEquals(20.0, product.getPrice());
        assertEquals(1, product.getUncommittedEvents().size());
        assertTrue(product.getUncommittedEvents().get(0) instanceof ProductUpdatedEvent);
    }
}
