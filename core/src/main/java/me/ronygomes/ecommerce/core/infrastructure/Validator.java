package me.ronygomes.ecommerce.core.infrastructure;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

import java.util.Set;
import java.util.stream.Collectors;

public class Validator {

    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();

    public void validate(Object command) {
        Set<ConstraintViolation<Object>> violations = factory.getValidator().validate(command);

        if (!violations.isEmpty()) {
            Set<String> errorMessages = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.toSet());

            throw new ValidationException(String.join("\n", errorMessages));
        }
    }
}
