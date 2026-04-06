package com.alejandro.banking_api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
        Long id,
        String accountNumber,
        String accountType,
        BigDecimal balance,
        boolean active,
        LocalDateTime createdAt
) {
}
