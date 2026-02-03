package me.ronygomes.ecommerce.ordering.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record OrderNumber(String value) {
    public static OrderNumber generate() {
        // Simple generation logic for MVP: ORD-YYYYMMDD-Random
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%04d", (int) (Math.random() * 10000));
        return new OrderNumber("ORD-" + datePart + "-" + randomPart);
    }
}
