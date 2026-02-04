package me.ronygomes.ecommerce.core.infrastructure;

public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
