package org.example.modular_store.order;

import java.math.BigDecimal;

public record OrderPlacedEvent(
        Long orderId,
        Long customerId,
        BigDecimal amount
) {}
