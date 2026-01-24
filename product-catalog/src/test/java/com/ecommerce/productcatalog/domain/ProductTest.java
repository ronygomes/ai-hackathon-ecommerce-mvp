package com.ecommerce.productcatalog.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ProductTest {
    @Test
    public void createProduct_withValidData_shouldSucceed() {
        Product product = Product.create(new Sku("SKU-123"), new ProductName("Test Product"), new Price(99.99),
                new ProductDescription("Description"));

        assertNotNull(product.getId());
        assertEquals("Test Product", product.getName().value());
        assertEquals("SKU-123", product.getSku().value());
        assertEquals(99.99, product.getPrice().value());
        assertEquals(1, product.getUncommittedEvents().size());
        assertTrue(product.getUncommittedEvents().get(0) instanceof ProductCreated);
    }

    @Test
    public void createProduct_withNegativePrice_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Price(-1.0);
        });
    }

    @Test
    public void updateProduct_withValidPrice_shouldSucceed() {
        Product product = Product.create(new Sku("SKU"), new ProductName("Test"), new Price(10.0),
                new ProductDescription("Desc"));
        product.clearUncommittedEvents();

        product.updateDetails(new ProductName("New Name"), new ProductDescription("New Desc"));

        assertEquals("New Name", product.getName().value());
        assertEquals(1, product.getUncommittedEvents().size());
        assertTrue(product.getUncommittedEvents().get(0) instanceof ProductDetailsUpdated);
    }
}
