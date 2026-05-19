package me.ronygomes.ecommerce.core.infrastructure;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ValidatorTest {

    private record Sample(@NotBlank(message = "name required") String name,
                          @Min(value = 1, message = "qty must be >= 1") int qty) {
    }

    private final Validator validator = new Validator();

    @Test
    void validate_validObject_doesNotThrow() {
        assertThatCode(() -> validator.validate(new Sample("ok", 1)))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_singleViolation_throwsValidationExceptionWithMessage() {
        assertThatThrownBy(() -> validator.validate(new Sample("", 1)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("name required");
    }

    @Test
    void validate_multipleViolations_joinsMessagesWithNewline() {
        assertThatThrownBy(() -> validator.validate(new Sample("", 0)))
                .isInstanceOf(ValidationException.class)
                .satisfies(thrown -> assertThat(thrown.getMessage().split("\n"))
                        .containsExactlyInAnyOrder("name required", "qty must be >= 1"));
    }
}
