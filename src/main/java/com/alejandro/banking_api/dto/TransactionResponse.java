package com.alejandro.banking_api.dto;

import com.alejandro.banking_api.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        TransactionType type,
        BigDecimal amount,
        String description,
        LocalDateTime createdAt
) {
}
