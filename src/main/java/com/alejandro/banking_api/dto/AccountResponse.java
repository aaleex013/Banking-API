package com.alejandro.banking_api.dto;

import com.alejandro.banking_api.entity.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
        Long id,
        String accountNumber,
        AccountType accountType,
        BigDecimal balance,
        boolean active,
        LocalDateTime createdAt
) {
}
